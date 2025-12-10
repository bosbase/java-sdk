package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.ClientResponseError;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class PubSubService extends BaseService {
    public static class PubSubMessage<T> {
        public final String id;
        public final String topic;
        public final String created;
        public final T data;

        public PubSubMessage(String id, String topic, String created, T data) {
            this.id = id;
            this.topic = topic;
            this.created = created;
            this.data = data;
        }
    }

    public static class PublishAck {
        public final String id;
        public final String topic;
        public final String created;

        public PublishAck(String id, String topic, String created) {
            this.id = id;
            this.topic = topic;
            this.created = created;
        }
    }

    public static class RealtimeMessage<T> {
        public final String topic;
        public final String event;
        public final T payload;
        public final String ref;
        public final String id;
        public final String created;

        public RealtimeMessage(String topic, String event, T payload, String ref, String id, String created) {
            this.topic = topic;
            this.event = event;
            this.payload = payload;
            this.ref = ref;
            this.id = id;
            this.created = created;
        }
    }

    private static class PendingAck {
        final Consumer<Map<String, Object>> resolve;
        final Consumer<Throwable> reject;
        final ScheduledFuture<?> timeout;

        PendingAck(Consumer<Map<String, Object>> resolve, Consumer<Throwable> reject, ScheduledFuture<?> timeout) {
            this.resolve = resolve;
            this.reject = reject;
            this.timeout = timeout;
        }
    }

    private final ObjectMapper mapper = JsonUtils.MAPPER;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "bosbase-pubsub"));

    private final List<CompletableFuture<Void>> pendingConnects = new ArrayList<>();
    private final ConcurrentHashMap<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Consumer<PubSubMessage<Object>>>> subscriptions = new ConcurrentHashMap<>();

    private final long[] predefinedReconnectIntervals = new long[] {200L, 300L, 500L, 1000L, 1200L, 1500L, 2000L};
    private final long ackTimeoutMs = 10_000L;
    private final long maxConnectTimeout = 15_000L;

    private WebSocket socket;
    private ScheduledFuture<?> connectTimeout;
    private ScheduledFuture<?> reconnectTimeout;
    private int reconnectAttempts = 0;
    private boolean manualClose = false;
    private boolean isReady = false;
    private String clientId = "";
    private final Object lock = new Object();

    public PubSubService(BosBase client) {
        super(client);
    }

    public boolean isConnected() {
        synchronized (lock) {
            return socket != null && isReady;
        }
    }

    public PublishAck publish(String topic, Object data) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic must be set.");
        ensureSocket().join();

        String requestId = nextRequestId();
        CompletableFuture<PublishAck> ackFuture = waitForAck(requestId, payload -> new PublishAck(
            payload.getOrDefault("id", "").toString(),
            payload.getOrDefault("topic", topic).toString(),
            payload.getOrDefault("created", "").toString()
        ));

        sendEnvelope(Map.of(
            "type", "publish",
            "topic", topic,
            "data", data,
            "requestId", requestId
        ));

        try {
            return ackFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Publish a realtime message envelope `{topic, event, payload, ref}` over the pub/sub websocket.
     */
    public PublishAck realtimePublish(String topic, String event, Object payload, String ref) {
        if (event == null || event.trim().isEmpty()) {
            throw new IllegalArgumentException("event must be set.");
        }
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("event", event);
        envelope.put("payload", payload);
        if (ref != null && !ref.isBlank()) {
            envelope.put("ref", ref);
        }
        return publish(topic, envelope);
    }

    public Runnable subscribe(String topic, Consumer<PubSubMessage<Object>> callback) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic must be set.");

        boolean isFirstListener;
        synchronized (lock) {
            Set<Consumer<PubSubMessage<Object>>> listeners = subscriptions.computeIfAbsent(topic, __ -> ConcurrentHashMap.newKeySet());
            isFirstListener = listeners.isEmpty();
            listeners.add(callback);
        }

        ensureSocket().join();

        if (isFirstListener) {
            String requestId = nextRequestId();
            waitForAck(requestId, payload -> true);
            sendEnvelope(Map.of(
                "type", "subscribe",
                "topic", topic,
                "requestId", requestId
            ));
        }

        return () -> {
            boolean shouldDisconnect = false;
            boolean shouldUnsubscribe = false;
            synchronized (lock) {
                Set<Consumer<PubSubMessage<Object>>> listeners = subscriptions.get(topic);
                if (listeners != null) {
                    listeners.remove(callback);
                    if (listeners.isEmpty()) {
                        subscriptions.remove(topic);
                        shouldUnsubscribe = true;
                        shouldDisconnect = subscriptions.isEmpty();
                    }
                }
            }

            if (shouldUnsubscribe) {
                try {
                    sendUnsubscribe(topic);
                } catch (Exception ignored) {
                }
            }

            if (shouldDisconnect) {
                disconnect();
            }
        };
    }

    /**
     * Subscribe to realtime messages emitted on a topic.
     */
    public Runnable realtimeSubscribe(String topic, Consumer<RealtimeMessage<Object>> callback) {
        if (callback == null) throw new IllegalArgumentException("callback must be set.");
        return subscribe(topic, msg -> {
            Object data = msg.data;
            String event = "";
            Object payload = data;
            String ref = null;

            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                Object maybeEvent = map.get("event");
                if (maybeEvent instanceof String) {
                    event = (String) maybeEvent;
                }
                if (map.containsKey("payload")) {
                    payload = map.get("payload");
                }
                Object maybeRef = map.get("ref");
                if (maybeRef instanceof String) {
                    ref = (String) maybeRef;
                }
            }

            RealtimeMessage<Object> normalized = new RealtimeMessage<>(
                msg.topic != null ? msg.topic : topic,
                event,
                payload,
                ref,
                msg.id,
                msg.created
            );
            callback.accept(normalized);
        });
    }

    public void unsubscribe(String topic) {
        List<String> topicsToRemove;
        synchronized (lock) {
            topicsToRemove = topic == null ? new ArrayList<>(subscriptions.keySet()) : subscriptions.keySet().stream()
                .filter(t -> t.equals(topic))
                .collect(Collectors.toList());
            topicsToRemove.forEach(subscriptions::remove);
        }

        if (topicsToRemove.isEmpty()) {
            return;
        }

        if (topic == null) {
            sendEnvelope(Map.of("type", "unsubscribe"));
            disconnect();
            return;
        }

        sendUnsubscribe(topic);
        if (!hasSubscriptions()) {
            disconnect();
        }
    }

    public void disconnect() {
        manualClose = true;
        rejectAllPending(new RuntimeException("pubsub connection closed"));
        closeSocket(false);
        synchronized (lock) {
            pendingConnects.clear();
        }
    }

    private boolean hasSubscriptions() {
        synchronized (lock) {
            return !subscriptions.isEmpty();
        }
    }

    private String buildWebSocketURL() {
        Map<String, Object> query = new java.util.HashMap<>();
        if (client.authStore.getToken() != null) {
            query.put("token", client.authStore.getToken());
        }

        okhttp3.HttpUrl httpUrl = client.buildUrl("/api/pubsub", query);
        String scheme = httpUrl.scheme().equalsIgnoreCase("https") ? "wss" : "ws";
        return httpUrl.newBuilder().scheme(scheme).build().toString();
    }

    private String nextRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private CompletableFuture<Void> ensureSocket() {
        synchronized (lock) {
            if (isReady && socket != null) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<Void> future = new CompletableFuture<>();
            pendingConnects.add(future);

            if (pendingConnects.size() == 1) {
                initConnect();
            }

            return future;
        }
    }

    private void initConnect() {
        closeSocket(true);
        manualClose = false;
        isReady = false;

        String url;
        try {
            url = buildWebSocketURL();
        } catch (Throwable err) {
            connectErrorHandler(err);
            return;
        }

        Request request = new Request.Builder().url(url).build();
        try {
            socket = client.httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleMessage(text);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    handleClose();
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                    connectErrorHandler(t);
                }
            });
        } catch (Throwable err) {
            connectErrorHandler(err);
            return;
        }

        connectTimeout = scheduler.schedule(
            () -> connectErrorHandler(new RuntimeException("WebSocket connect took too long.")),
            maxConnectTimeout,
            TimeUnit.MILLISECONDS
        );
    }

    private void handleMessage(String payload) {
        if (connectTimeout != null) connectTimeout.cancel(true);

        JsonNode data;
        try {
            data = mapper.readTree(payload);
        } catch (Exception e) {
            return;
        }
        if (data == null || !data.isObject()) return;

        ObjectNode obj = (ObjectNode) data;
        String type = obj.path("type").asText(null);
        if (type == null) return;

        switch (type) {
            case "ready":
                clientId = obj.path("clientId").asText("");
                handleConnected();
                break;
            case "message":
                String topic = obj.path("topic").asText(null);
                if (topic == null) return;
                List<Consumer<PubSubMessage<Object>>> listeners = synchronizedListeners(topic);
                PubSubMessage<Object> message = new PubSubMessage<>(
                    obj.path("id").asText(""),
                    topic,
                    obj.path("created").asText(""),
                    decodeValue(obj.get("data"))
                );
                for (Consumer<PubSubMessage<Object>> listener : listeners) {
                    try {
                        listener.accept(message);
                    } catch (Exception ignored) {
                    }
                }
                break;
            case "published":
            case "subscribed":
            case "unsubscribed":
            case "pong":
                Map<String, Object> map = mapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
                String requestId = obj.path("requestId").asText(null);
                if (requestId != null) {
                    resolvePending(requestId, map);
                }
                break;
            case "error":
                String errMessage = obj.path("message").asText("pubsub error");
                String errRequestId = obj.path("requestId").asText(null);
                RuntimeException err = new RuntimeException(errMessage);
                if (errRequestId != null) {
                    rejectPending(errRequestId, err);
                }
                break;
            default:
                break;
        }
    }

    private void handleConnected() {
        boolean shouldResubscribe = reconnectAttempts > 0;
        reconnectAttempts = 0;
        isReady = true;
        if (reconnectTimeout != null) reconnectTimeout.cancel(true);
        if (connectTimeout != null) connectTimeout.cancel(true);

        List<CompletableFuture<Void>> waiters;
        synchronized (lock) {
            waiters = new ArrayList<>(pendingConnects);
            pendingConnects.clear();
        }
        waiters.forEach(f -> f.complete(null));

        if (shouldResubscribe) {
            List<String> topics = new ArrayList<>();
            synchronized (lock) {
                topics.addAll(subscriptions.keySet());
            }
            for (String topic : topics) {
                String requestId = nextRequestId();
                sendEnvelope(Map.of(
                    "type", "subscribe",
                    "topic", topic,
                    "requestId", requestId
                ));
            }
        }
    }

    private void handleClose() {
        socket = null;
        isReady = false;

        if (manualClose) {
            return;
        }

        rejectAllPending(new RuntimeException("pubsub connection closed"));

        if (!hasSubscriptions()) {
            synchronized (lock) {
                pendingConnects.clear();
            }
            return;
        }

        long delay = reconnectAttempts < predefinedReconnectIntervals.length
            ? predefinedReconnectIntervals[reconnectAttempts]
            : predefinedReconnectIntervals[predefinedReconnectIntervals.length - 1];
        if (reconnectAttempts < Integer.MAX_VALUE) {
            reconnectAttempts++;
            if (reconnectTimeout != null) reconnectTimeout.cancel(true);
            reconnectTimeout = scheduler.schedule(this::initConnect, delay, TimeUnit.MILLISECONDS);
        }
    }

    private void sendEnvelope(Map<String, Object> data) {
        if (!isReady || socket == null) {
            ensureSocket().join();
        }
        String payload;
        try {
            payload = mapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialize websocket payload", e);
        }
        WebSocket ws = socket;
        if (ws == null) throw new RuntimeException("Unable to send websocket message - socket not initialized.");
        ws.send(payload);
    }

    private void sendUnsubscribe(String topic) {
        String requestId = nextRequestId();
        waitForAck(requestId, payload -> true);
        sendEnvelope(Map.of(
            "type", "unsubscribe",
            "topic", topic,
            "requestId", requestId
        ));
    }

    private void connectErrorHandler(Throwable err) {
        if (connectTimeout != null) connectTimeout.cancel(true);

        if (reconnectAttempts > Integer.MAX_VALUE - 1 || manualClose) {
            List<CompletableFuture<Void>> waiters;
            synchronized (lock) {
                waiters = new ArrayList<>(pendingConnects);
                pendingConnects.clear();
            }
            ClientResponseError error = new ClientResponseError("", err);
            waiters.forEach(f -> f.completeExceptionally(error));
            closeSocket(false);
            return;
        }

        closeSocket(true);
        long delay = reconnectAttempts < predefinedReconnectIntervals.length
            ? predefinedReconnectIntervals[reconnectAttempts]
            : predefinedReconnectIntervals[predefinedReconnectIntervals.length - 1];
        reconnectAttempts++;
        if (reconnectTimeout != null) reconnectTimeout.cancel(true);
        reconnectTimeout = scheduler.schedule(this::initConnect, delay, TimeUnit.MILLISECONDS);
    }

    private void closeSocket(boolean keepSubscriptions) {
        try {
            if (socket != null) socket.cancel();
        } catch (Exception ignored) {
        }
        socket = null;
        isReady = false;

        if (connectTimeout != null) connectTimeout.cancel(true);
        if (reconnectTimeout != null) reconnectTimeout.cancel(true);

        if (!keepSubscriptions) {
            subscriptions.clear();
            pendingAcks.values().forEach(p -> p.timeout.cancel(true));
            pendingAcks.clear();
        }
    }

    private <T> CompletableFuture<T> waitForAck(String requestId, java.util.function.Function<Map<String, Object>, T> mapperFunc) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ScheduledFuture<?> timeout = scheduler.schedule(
            () -> {
                pendingAcks.remove(requestId);
                future.completeExceptionally(new RuntimeException("Timed out waiting for pubsub response."));
            },
            ackTimeoutMs,
            TimeUnit.MILLISECONDS
        );

        pendingAcks.put(requestId, new PendingAck(
            payload -> {
                try {
                    T result = mapperFunc != null ? mapperFunc.apply(payload) : null;
                    future.complete(result);
                } catch (Throwable err) {
                    future.completeExceptionally(err);
                }
            },
            future::completeExceptionally,
            timeout
        ));

        return future;
    }

    private void resolvePending(String requestId, Map<String, Object> payload) {
        PendingAck pending = pendingAcks.remove(requestId);
        if (pending == null) return;
        if (pending.timeout != null) pending.timeout.cancel(true);
        pending.resolve.accept(payload);
    }

    private void rejectPending(String requestId, Throwable err) {
        PendingAck pending = pendingAcks.remove(requestId);
        if (pending == null) return;
        if (pending.timeout != null) pending.timeout.cancel(true);
        pending.reject.accept(err);
    }

    private void rejectAllPending(Throwable err) {
        pendingAcks.values().forEach(p -> {
            if (p.timeout != null) p.timeout.cancel(true);
            p.reject.accept(err);
        });
        pendingAcks.clear();

        List<CompletableFuture<Void>> waiters;
        synchronized (lock) {
            waiters = new ArrayList<>(pendingConnects);
            pendingConnects.clear();
        }
        waiters.forEach(f -> f.completeExceptionally(err));
    }

    private List<Consumer<PubSubMessage<Object>>> synchronizedListeners(String topic) {
        synchronized (lock) {
            Set<Consumer<PubSubMessage<Object>>> listeners = subscriptions.get(topic);
            if (listeners == null) return List.of();
            return new ArrayList<>(listeners);
        }
    }

    private Object decodeValue(JsonNode element) {
        if (element == null || element.isNull()) return null;
        if (element.isObject()) {
            return mapper.convertValue(element, new TypeReference<Map<String, Object>>() {});
        }
        if (element.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode node : (ArrayNode) element) {
                list.add(decodeValue(node));
            }
            return list;
        }
        if (element.isBoolean()) return element.booleanValue();
        if (element.isNumber()) return element.numberValue();
        if (element.isTextual()) return element.textValue();
        return element.toString();
    }
}
