package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * PluginService forwards requests to the configured plugin proxy endpoint.
 */
public class PluginService extends BaseService {
    private static final Set<String> PLUGIN_HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private static final Set<String> PLUGIN_SSE_METHODS = Set.of("SSE");
    private static final Set<String> PLUGIN_WS_METHODS = Set.of("WS", "WEBSOCKET");

    public PluginService(BosBase client) {
        super(client);
    }

    /**
     * Send a HTTP request to the plugin proxy endpoint.
     */
    public JsonNode request(String method, String path, Map<String, String> headers, Map<String, Object> query, Object body, Long timeoutSeconds, String requestKey, Boolean autoCancel) {
        String normalizedMethod = method != null ? method.trim().toUpperCase(Locale.US) : "";
        if (PLUGIN_SSE_METHODS.contains(normalizedMethod) || PLUGIN_WS_METHODS.contains(normalizedMethod)) {
            throw new IllegalArgumentException("Use sse() or websocket() for SSE/WebSocket plugin requests.");
        }
        if (!PLUGIN_HTTP_METHODS.contains(normalizedMethod)) {
            throw new IllegalArgumentException("Unsupported plugin method \"" + method + "\"");
        }

        String targetPath = normalizePath(path);
        return client.send(targetPath, normalizedMethod, headers, query, body, null, timeoutSeconds, requestKey, autoCancel == null || autoCancel);
    }

    /**
     * Opens a plugin SSE stream.
     */
    public EventSource sse(String path, EventSourceListener listener, Map<String, Object> query, Map<String, String> headers) {
        if (listener == null) throw new IllegalArgumentException("listener is required");

        HttpUrl url = buildUrl(normalizePath(path), query, true);
        Request.Builder reqBuilder = new Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-store")
            .header("Accept-Language", client.lang)
            .header("User-Agent", "bosbase-java-sdk");

        if (client.authStore != null && client.authStore.getToken() != null) {
            reqBuilder.header("Authorization", client.authStore.getToken());
        }
        if (headers != null) {
            headers.forEach(reqBuilder::header);
        }

        EventSource.Factory factory = EventSources.createFactory(client.httpClient);
        return factory.newEventSource(reqBuilder.build(), listener);
    }

    /**
     * Opens a plugin WebSocket connection.
     */
    public WebSocket websocket(String path, WebSocketListener listener, Map<String, Object> query, Map<String, String> headers) {
        if (listener == null) throw new IllegalArgumentException("listener is required");

        HttpUrl httpUrl = buildUrl(normalizePath(path), query, true);
        String scheme = httpUrl.scheme().equalsIgnoreCase("https") ? "wss" : "ws";
        HttpUrl wsUrl = httpUrl.newBuilder().scheme(scheme).build();

        Request.Builder reqBuilder = new Request.Builder().url(wsUrl);
        if (client.authStore != null && client.authStore.getToken() != null) {
            reqBuilder.header("Authorization", client.authStore.getToken());
        }
        if (headers != null) {
            headers.forEach(reqBuilder::header);
        }

        return client.httpClient.newWebSocket(reqBuilder.build(), listener);
    }

    private String normalizePath(String path) {
        String normalized = path == null ? "" : path.replaceFirst("^/+", "");
        if (normalized.isBlank()) return "/api/plugins";
        if (normalized.startsWith("api/plugins")) return "/" + normalized;
        if (normalized.startsWith("plugins")) return "/api/" + normalized;
        return "/api/plugins/" + normalized;
    }

    private HttpUrl buildUrl(String path, Map<String, Object> query, boolean includeToken) {
        Map<String, Object> params = new HashMap<>();
        if (query != null) params.putAll(query);
        if (includeToken && client.authStore != null && client.authStore.getToken() != null) {
            params.putIfAbsent("token", client.authStore.getToken());
        }

        return client.buildUrl(path, params);
    }
}
