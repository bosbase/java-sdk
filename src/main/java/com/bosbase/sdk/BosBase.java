package com.bosbase.sdk;

import com.bosbase.sdk.services.BackupService;
import com.bosbase.sdk.services.BatchService;
import com.bosbase.sdk.services.CacheService;
import com.bosbase.sdk.services.CollectionService;
import com.bosbase.sdk.services.CronService;
import com.bosbase.sdk.services.FileService;
import com.bosbase.sdk.services.GraphQLService;
import com.bosbase.sdk.services.HealthService;
import com.bosbase.sdk.services.LLMDocumentService;
import com.bosbase.sdk.services.LangChaingoService;
import com.bosbase.sdk.services.LogService;
import com.bosbase.sdk.services.PubSubService;
import com.bosbase.sdk.services.RecordService;
import com.bosbase.sdk.services.RealtimeService;
import com.bosbase.sdk.services.SettingsService;
import com.bosbase.sdk.services.VectorService;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BosBase {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String USER_AGENT = "bosbase-java-sdk/0.1.0";

    private static class AutoRefreshState {
        final long thresholdSeconds;
        final Runnable refreshFunc;
        final Runnable reauthenticateFunc;
        final BeforeSendHook originalBeforeSend;
        final String initialRecordId;
        final String initialCollectionId;

        AutoRefreshState(
            long thresholdSeconds,
            Runnable refreshFunc,
            Runnable reauthenticateFunc,
            BeforeSendHook originalBeforeSend,
            String initialRecordId,
            String initialCollectionId
        ) {
            this.thresholdSeconds = thresholdSeconds;
            this.refreshFunc = refreshFunc;
            this.reauthenticateFunc = reauthenticateFunc;
            this.originalBeforeSend = originalBeforeSend;
            this.initialRecordId = initialRecordId;
            this.initialCollectionId = initialCollectionId;
        }
    }

    @FunctionalInterface
    public interface BeforeSendHook {
        BeforeSendResult apply(String url, RequestOptions options) throws Exception;
    }

    @FunctionalInterface
    public interface AfterSendHook {
        JsonNode apply(Response response, JsonNode data, RequestOptions options) throws Exception;
    }

    public final String baseUrl;
    public String lang;
    public BaseAuthStore authStore;
    public final OkHttpClient httpClient;

    public BeforeSendHook beforeSend;
    public AfterSendHook afterSend;

    public final CollectionService collections;
    public final FileService files;
    public final LogService logs;
    public final RealtimeService realtime;
    public final PubSubService pubsub;
    public final HealthService health;
    public final BackupService backups;
    public final CronService crons;
    public final VectorService vectors;
    public final LLMDocumentService llmDocuments;
    public final LangChaingoService langchaingo;
    public final CacheService caches;
    public final GraphQLService graphql;
    public final SettingsService settings;

    private final ObjectMapper mapper = JsonUtils.MAPPER;
    private final Map<String, RecordService> recordServices = new ConcurrentHashMap<>();
    private final Map<String, Call> cancelCalls = new ConcurrentHashMap<>();
    private AutoRefreshState autoRefreshState;
    private boolean enableAutoCancellation = true;

    public BosBase(String baseUrl) {
        this(baseUrl, "en-US", null, null);
    }

    public BosBase(String baseUrl, String lang, BaseAuthStore authStore, OkHttpClient client) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.lang = lang != null ? lang : "en-US";
        if (authStore != null) {
            this.authStore = authStore;
        } else {
            this.authStore = new LocalAuthStore();
        }
        this.httpClient = client != null ? client : new OkHttpClient();

        this.collections = new CollectionService(this);
        this.files = new FileService(this);
        this.logs = new LogService(this);
        this.realtime = new RealtimeService(this);
        this.pubsub = new PubSubService(this);
        this.health = new HealthService(this);
        this.backups = new BackupService(this);
        this.crons = new CronService(this);
        this.vectors = new VectorService(this);
        this.llmDocuments = new LLMDocumentService(this);
        this.langchaingo = new LangChaingoService(this);
        this.caches = new CacheService(this);
        this.graphql = new GraphQLService(this);
        this.settings = new SettingsService(this);
    }

    public RecordService admins() {
        return collection("_superusers");
    }

    public RecordService collection(String idOrName) {
        return recordServices.computeIfAbsent(idOrName, key -> new RecordService(this, key));
    }

    public BatchService createBatch() {
        return new BatchService(this);
    }

    public BosBase autoCancellation(boolean enable) {
        this.enableAutoCancellation = enable;
        return this;
    }

    public BosBase cancelRequest(String requestKey) {
        Call call = cancelCalls.remove(requestKey);
        if (call != null) {
            call.cancel();
        }
        return this;
    }

    public BosBase cancelAllRequests() {
        for (Call call : cancelCalls.values()) {
            call.cancel();
        }
        cancelCalls.clear();
        return this;
    }

    public void registerAutoRefresh(long thresholdSeconds, Runnable refreshFunc, Runnable reauthenticateFunc) {
        resetAutoRefresh();

        ObjectNode initialModel = authStore.getModel();
        String initialRecordId = initialModel != null ? Optional.ofNullable(initialModel.get("id")).map(JsonNode::asText).orElse(null) : null;
        String initialCollectionId = initialModel != null ? Optional.ofNullable(initialModel.get("collectionId")).map(JsonNode::asText).orElse(null) : null;
        BeforeSendHook originalBefore = beforeSend;

        autoRefreshState = new AutoRefreshState(
            thresholdSeconds,
            refreshFunc,
            reauthenticateFunc,
            originalBefore,
            initialRecordId,
            initialCollectionId
        );

        beforeSend = (url, options) -> {
            AutoRefreshState state = autoRefreshState;
            if (state == null) {
                return originalBefore != null ? originalBefore.apply(url, options) : null;
            }

            ObjectNode model = authStore.getModel();
            String currentRecordId = model != null ? Optional.ofNullable(model.get("id")).map(JsonNode::asText).orElse(null) : null;
            String currentCollectionId = model != null ? Optional.ofNullable(model.get("collectionId")).map(JsonNode::asText).orElse(null) : null;
            if (state.initialRecordId != null &&
                (currentRecordId == null ||
                    !state.initialRecordId.equals(currentRecordId) ||
                    !Objects.equals(state.initialCollectionId, currentCollectionId))) {
                resetAutoRefresh();
                return originalBefore != null ? originalBefore.apply(url, options) : new BeforeSendResult(null, options);
            }

            boolean skipAutoRefresh = Optional.ofNullable(options.query.get("autoRefresh"))
                .map(Object::toString)
                .map(Boolean::parseBoolean)
                .orElse(false);
            String previousToken = authStore.getToken();
            if (!skipAutoRefresh) {
                boolean isValid = authStore.isValid();
                if (isValid && isTokenExpiring(authStore.getToken(), state.thresholdSeconds)) {
                    try {
                        state.refreshFunc.run();
                    } catch (Exception ignored) {
                        isValid = authStore.isValid();
                    }
                }

                if (!authStore.isValid() && !isValid) {
                    try {
                        state.reauthenticateFunc.run();
                    } catch (Exception ignored) {
                    }
                }

                syncAuthorizationHeader(options, previousToken);
            }

            return originalBefore != null ? originalBefore.apply(url, options) : new BeforeSendResult(null, options);
        };
    }

    public void resetAutoRefresh() {
        AutoRefreshState state = autoRefreshState;
        if (state != null) {
            beforeSend = state.originalBeforeSend;
            autoRefreshState = null;
        }
    }

    public String filter(String raw, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return raw;
        String result = raw;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{:" + entry.getKey() + "}";
            Object val = entry.getValue();
            String replacement;
            if (val == null) {
                replacement = "null";
            } else if (val instanceof Boolean || val instanceof Number) {
                replacement = val.toString();
            } else if (val instanceof String) {
                replacement = "'" + ((String) val).replace("'", "\\'") + "'";
            } else if (val instanceof java.util.Date) {
                String iso = ((Date) val).toInstant().toString().replace("T", " ");
                replacement = "'" + iso + "'";
            } else if (val instanceof TemporalAccessor) {
                replacement = "'" + val.toString() + "'";
            } else {
                try {
                    replacement = "'" + mapper.writeValueAsString(val).replace("'", "\\'") + "'";
                } catch (JsonProcessingException e) {
                    replacement = "'" + val.toString().replace("'", "\\'") + "'";
                }
            }
            result = result.replace(placeholder, replacement);
        }
        return result;
    }

    public HttpUrl buildUrl(String path) {
        return buildUrl(path, null);
    }

    public HttpUrl buildUrl(String path, Map<String, Object> query) {
        String target = baseUrl + "/" + path.replaceFirst("^/", "");
        Builder builder = HttpUrl.parse(target) != null ? Objects.requireNonNull(HttpUrl.parse(target)).newBuilder() : null;
        if (builder == null) {
            throw new IllegalArgumentException("Invalid URL: " + target);
        }

        if (query != null) {
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                appendQuery(builder, entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public JsonNode send(
        String path,
        String method,
        Map<String, String> headers,
        Map<String, Object> query,
        Object body,
        Map<String, java.util.List<FileAttachment>> files,
        Long timeoutSeconds,
        String requestKey,
        Boolean autoCancel
    ) {
        RequestOptions hookOptions = new RequestOptions();
        hookOptions.method = method != null ? method : "GET";
        if (headers != null) hookOptions.headers.putAll(headers);
        if (query != null) hookOptions.query.putAll(query);
        hookOptions.body = body;
        hookOptions.files = files;
        hookOptions.timeoutSeconds = timeoutSeconds;
        hookOptions.requestKey = requestKey;
        hookOptions.autoCancel = autoCancel == null ? true : autoCancel;

        HttpUrl urlBeforeHooks = buildUrl(path, hookOptions.query);
        Pair<RequestOptions, String> result = applyBeforeSend(urlBeforeHooks.toString(), hookOptions);
        RequestOptions finalOptions = result.first;
        String overrideUrl = result.second;

        normalizeRequestKey(finalOptions);

        HttpUrl targetUrl = overrideUrl != null ? HttpUrl.parse(overrideUrl) : buildUrl(path, finalOptions.query);
        if (targetUrl == null) {
            throw new IllegalArgumentException("Invalid URL produced for path " + path);
        }
        Request.Builder reqBuilder = new Request.Builder().url(targetUrl);

        Map<String, String> computedHeaders = new java.util.LinkedHashMap<>();
        computedHeaders.put("Accept-Language", lang);
        computedHeaders.put("User-Agent", USER_AGENT);
        if (authStore.isValid()) {
            String token = authStore.getToken();
            if (token != null) computedHeaders.put("Authorization", token);
        }
        computedHeaders.putAll(finalOptions.headers);
        for (Map.Entry<String, String> entry : computedHeaders.entrySet()) {
            reqBuilder.header(entry.getKey(), entry.getValue());
        }

        RequestBody requestBody = buildRequestBody(finalOptions.body, finalOptions.files);
        String upperMethod = finalOptions.method != null ? finalOptions.method.trim().toUpperCase(Locale.US) : "GET";
        if (requestBody != null) {
            reqBuilder.method(upperMethod, requestBody);
        } else {
            reqBuilder.method(upperMethod, requiresRequestBody(upperMethod) ? RequestBody.create(new byte[0], null) : null);
        }

        String cancelKey = null;
        if (enableAutoCancellation && finalOptions.autoCancel) {
            cancelKey = finalOptions.requestKey != null ? finalOptions.requestKey : upperMethod + " " + path;
            cancelRequest(cancelKey);
        }

        OkHttpClient clientToUse = timeoutSeconds != null
            ? httpClient.newBuilder().callTimeout(timeoutSeconds, TimeUnit.SECONDS).build()
            : httpClient;

        Call call = clientToUse.newCall(reqBuilder.build());
        if (cancelKey != null) {
            cancelCalls.put(cancelKey, call);
        }

        Response response;
        try {
            response = call.execute();
        } catch (IOException io) {
            throw new ClientResponseError(targetUrl.toString(), null, Collections.emptyMap(), io instanceof java.io.InterruptedIOException, io);
        } finally {
            if (cancelKey != null) {
                cancelCalls.remove(cancelKey);
            }
        }

        try (Response resp = response) {
            int status = resp.code();
            String contentType = Optional.ofNullable(resp.header("Content-Type")).orElse("").toLowerCase(Locale.US);
            String rawBody = resp.body() != null ? resp.body().string() : "";
            JsonNode data;
            if (status == 204 || rawBody.isEmpty()) {
                data = NullNode.getInstance();
            } else if (contentType.contains("application/json")) {
                try {
                    data = mapper.readTree(rawBody);
                } catch (Exception e) {
                    data = NullNode.getInstance();
                }
            } else {
                data = mapper.getNodeFactory().textNode(rawBody);
            }

            if (status >= 400) {
                throw new ClientResponseError(
                    targetUrl.toString(),
                    status,
                    JsonUtils.jsonNodeToMap(data),
                    false,
                    null
                );
            }

            JsonNode resultData = afterSend != null ? afterSend.apply(resp, data, finalOptions) : data;
            return resultData == null ? NullNode.getInstance() : resultData;
        } catch (ClientResponseError cre) {
            throw cre;
        } catch (Exception err) {
            throw new ClientResponseError(targetUrl.toString(), err);
        }
    }

    public JsonNode send(String path, Map<String, Object> query, Map<String, String> headers) {
        return send(path, "GET", headers, query, null, null, null, null, true);
    }

    public JsonNode send(String path, Map<String, Object> query) {
        return send(path, "GET", null, query, null, null, null, null, true);
    }

    public JsonNode send(String path) {
        return send(path, "GET", null, null, null, null, null, null, true);
    }

    private RequestBody buildRequestBody(Object body, Map<String, java.util.List<FileAttachment>> files) {
        JsonNode payloadNode = body == null ? null : JsonUtils.toJsonNode(body);

        if (files != null && !files.isEmpty()) {
            MultipartBody.Builder multipart = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (payloadNode != null && !payloadNode.isNull()) {
                try {
                    String jsonString = mapper.writeValueAsString(payloadNode);
                    multipart.addFormDataPart("@jsonPayload", jsonString);
                } catch (JsonProcessingException ignored) {
                }
            }
            for (Map.Entry<String, java.util.List<FileAttachment>> entry : files.entrySet()) {
                String key = entry.getKey();
                java.util.List<FileAttachment> attachments = entry.getValue();
                for (int i = 0; i < attachments.size(); i++) {
                    FileAttachment attachment = attachments.get(i);
                    String partKey = attachments.size() > 1 ? key + "[" + i + "]" : key;
                    RequestBody partBody = RequestBody.create(attachment.bytes, attachment.mediaType);
                    multipart.addFormDataPart(partKey, attachment.filename, partBody);
                }
            }
            return multipart.build();
        }

        if (payloadNode != null && !payloadNode.isNull()) {
            try {
                String jsonString = mapper.writeValueAsString(payloadNode);
                return RequestBody.create(jsonString.getBytes(StandardCharsets.UTF_8), MEDIA_TYPE_JSON);
            } catch (JsonProcessingException ignored) {
            }
        }
        return null;
    }

    private Pair<RequestOptions, String> applyBeforeSend(String url, RequestOptions options) {
        if (beforeSend == null) {
            return new Pair<>(options, null);
        }
        try {
            BeforeSendResult override = beforeSend.apply(url, options);
            if (override == null) {
                return new Pair<>(options, null);
            }

            String targetUrl = override.getUrl();
            RequestOptions opts = override.getOptions() != null ? override.getOptions() : options;

            if (targetUrl != null) {
                HttpUrl parsed = HttpUrl.parse(targetUrl);
                if (parsed != null) {
                    Map<String, Object> rebuiltQuery = new java.util.LinkedHashMap<>();
                    for (String name : parsed.queryParameterNames()) {
                        java.util.List<String> values = parsed.queryParameterValues(name);
                        if (values.size() == 1) {
                            rebuiltQuery.put(name, values.get(0));
                        } else {
                            rebuiltQuery.put(name, values);
                        }
                    }
                    opts.query = rebuiltQuery;
                }
            }

            return new Pair<>(opts, targetUrl);
        } catch (Exception e) {
            throw new ClientResponseError(url, e);
        }
    }

    private void normalizeRequestKey(RequestOptions options) {
        Object autoCancelFlag = options.query.remove("$autoCancel");
        Object cancelKey = options.query.remove("$cancelKey");

        Object params = options.query.remove("params");
        if (params instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapParams = (Map<String, Object>) params;
            options.query.putAll(mapParams);
        }

        if (autoCancelFlag != null && "false".equalsIgnoreCase(autoCancelFlag.toString())) {
            options.autoCancel = false;
            return;
        }

        if (options.requestKey == null && cancelKey != null && !cancelKey.toString().isBlank()) {
            options.requestKey = cancelKey.toString();
        }
    }

    private void syncAuthorizationHeader(RequestOptions options, String previousToken) {
        if (previousToken == null || previousToken.isBlank()) return;
        for (Map.Entry<String, String> entry : options.headers.entrySet()) {
            if ("authorization".equalsIgnoreCase(entry.getKey()) && previousToken.equals(entry.getValue())) {
                String newToken = authStore.getToken();
                if (newToken != null) {
                    options.headers.put(entry.getKey(), newToken);
                }
                break;
            }
        }
    }

    private boolean isTokenExpiring(String token, long thresholdSeconds) {
        Long exp = getTokenExp(token);
        if (exp == null) return true;
        long threshold = thresholdSeconds < 0 ? 0 : thresholdSeconds;
        long now = System.currentTimeMillis() / 1000;
        return now >= exp - threshold;
    }

    private Long getTokenExp(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            byte[] payload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode obj = mapper.readTree(payload);
            JsonNode exp = obj.get("exp");
            return exp != null && exp.isNumber() ? exp.asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String padBase64(String raw) {
        int padding = (4 - raw.length() % 4) % 4;
        return raw + "=".repeat(padding);
    }

    private static void appendQuery(Builder builder, String key, Object value) {
        if (value == null) return;
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                appendQuery(builder, key, item);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                appendQuery(builder, key, java.lang.reflect.Array.get(value, i));
            }
            return;
        }
        builder.addQueryParameter(key, value.toString());
    }

    private boolean requiresRequestBody(String method) {
        return method != null && java.util.Set.of("POST", "PUT", "PATCH", "DELETE").contains(method.toUpperCase(Locale.US));
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) return "/";
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return trimmed.isEmpty() ? "/" : trimmed;
    }

    private static class Pair<F, S> {
        final F first;
        final S second;

        Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
}
