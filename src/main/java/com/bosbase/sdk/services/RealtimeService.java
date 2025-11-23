package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class RealtimeService extends BaseService {
    private final ObjectMapper mapper = JsonUtils.MAPPER;
    private final ConcurrentHashMap<String, List<Consumer<Map<String, Object>>>> subscriptions = new ConcurrentHashMap<>();
    private EventSource eventSource;
    private int reconnectAttempt = 0;
    private boolean manualDisconnect = false;
    private String clientId = "";

    private Consumer<List<String>> onDisconnect;

    public RealtimeService(BosBase client) {
        super(client);
    }

    public String getClientId() {
        return clientId;
    }

    public void setOnDisconnect(Consumer<List<String>> onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public Runnable subscribe(String topic, Consumer<Map<String, Object>> callback, Map<String, Object> query, Map<String, String> headers) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic must be set");
        String key = buildSubscriptionKey(topic, query, headers);
        List<Consumer<Map<String, Object>>> listeners = subscriptions.computeIfAbsent(key, __ -> Collections.synchronizedList(new ArrayList<>()));
        listeners.add(callback);

        manualDisconnect = false;
        if (eventSource == null) {
            connect();
        } else {
            submitSubscriptions();
        }

        return () -> unsubscribeByTopicAndListener(topic, callback);
    }

    public void unsubscribe(String topic) {
        if (topic == null) {
            subscriptions.clear();
            disconnect();
            return;
        }
        List<String> keys = subscriptions.keySet().stream()
            .filter(k -> k.equals(topic) || k.startsWith(topic + "?"))
            .collect(Collectors.toList());
        keys.forEach(subscriptions::remove);
        if (subscriptions.isEmpty()) {
            disconnect();
        } else {
            submitSubscriptions();
        }
    }

    public void unsubscribe() {
        unsubscribe(null);
    }

    public void unsubscribeByPrefix(String prefix) {
        List<String> keys = subscriptions.keySet().stream()
            .filter(k -> k.equals(prefix) || k.startsWith(prefix + "?"))
            .collect(Collectors.toList());
        keys.forEach(subscriptions::remove);
        if (subscriptions.isEmpty()) {
            disconnect();
        } else {
            submitSubscriptions();
        }
    }

    public void unsubscribeByTopicAndListener(String topic, Consumer<Map<String, Object>> listener) {
        List<String> keys = subscriptions.keySet().stream()
            .filter(k -> k.equals(topic) || k.startsWith(topic + "?"))
            .collect(Collectors.toList());
        for (String key : keys) {
            List<Consumer<Map<String, Object>>> listeners = subscriptions.get(key);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    subscriptions.remove(key);
                }
            }
        }
        if (subscriptions.isEmpty()) {
            disconnect();
        } else {
            submitSubscriptions();
        }
    }

    public void disconnect() {
        manualDisconnect = true;
        if (eventSource != null) {
            try {
                eventSource.cancel();
            } catch (Exception ignored) {
            }
        }
        eventSource = null;
        clientId = "";
    }

    private void connect() {
        if (manualDisconnect) return;
        Request.Builder reqBuilder = new Request.Builder()
            .url(client.buildUrl("/api/realtime").toString())
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-store")
            .header("Accept-Language", client.lang)
            .header("User-Agent", "bosbase-java-sdk");

        if (client.authStore.isValid() && client.authStore.getToken() != null) {
            reqBuilder.header("Authorization", client.authStore.getToken());
        }

        EventSource.Factory factory = EventSources.createFactory(client.httpClient);
        eventSource = factory.newEventSource(reqBuilder.build(), new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                String eventName = type != null ? type : "message";
                handleEvent(eventName, data, id);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                handleDisconnect();
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
                handleDisconnect();
            }
        });
    }

    private void handleEvent(String event, String data, String id) {
        if ("PB_CONNECT".equals(event)) {
            Map<String, Object> payload = parseJsonObject(data);
            Object cid = payload.get("clientId");
            this.clientId = cid != null ? cid.toString() : (id != null ? id : "");
            reconnectAttempt = 0;
            submitSubscriptions();
            return;
        }

        Map<String, Object> payload = parseJsonObject(data);
        List<Consumer<Map<String, Object>>> listeners = subscriptions.getOrDefault(event, List.of());
        for (Consumer<Map<String, Object>> listener : new ArrayList<>(listeners)) {
            try {
                listener.accept(payload);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleDisconnect() {
        List<String> active = new ArrayList<>(subscriptions.keySet());
        clientId = "";
        eventSource = null;
        if (onDisconnect != null) {
            try {
                onDisconnect.accept(active);
            } catch (Exception ignored) {
            }
        }
        if (!active.isEmpty() && !manualDisconnect) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        long[] delays = new long[] {200L, 500L, 1000L, 2000L, 5000L};
        long delay = reconnectAttempt < delays.length ? delays[reconnectAttempt] : delays[delays.length - 1];
        reconnectAttempt++;
        new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
            if (!subscriptions.isEmpty() && !manualDisconnect) {
                connect();
            }
        }).start();
    }

    private void submitSubscriptions() {
        if (clientId == null || clientId.isBlank() || subscriptions.isEmpty()) return;
        Map<String, Object> payload = Map.of(
            "clientId", clientId,
            "subscriptions", new ArrayList<>(subscriptions.keySet())
        );
        try {
            client.send("/api/realtime", "POST", null, null, payload, null, null, null, true);
        } catch (Exception ignored) {
        }
    }

    private String buildSubscriptionKey(String topic, Map<String, Object> query, Map<String, String> headers) {
        if ((query == null || query.isEmpty()) && (headers == null || headers.isEmpty())) {
            return topic;
        }
        Map<String, Object> opts = new java.util.LinkedHashMap<>();
        if (query != null) opts.put("query", query);
        if (headers != null) opts.put("headers", headers);
        String serialized;
        try {
            serialized = mapper.writeValueAsString(opts);
        } catch (Exception e) {
            serialized = "{}";
        }
        String encoded = URLEncoder.encode(serialized, StandardCharsets.UTF_8);
        return topic + (topic.contains("?") ? "&options=" : "?options=") + encoded;
    }

    private Map<String, Object> parseJsonObject(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            if (node != null && node.isObject()) {
                return mapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }
}
