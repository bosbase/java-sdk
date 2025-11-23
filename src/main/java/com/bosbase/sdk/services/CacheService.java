package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bosbase.sdk.PathUtils.encodePath;

public class CacheService extends BaseService {
    public CacheService(BosBase client) {
        super(client);
    }

    public List<ObjectNode> list(Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send("/api/cache", "GET", headers, query, null, null, null, null, true);
        if (data == null) return List.of();

        if (data.isArray()) {
            List<ObjectNode> result = new ArrayList<>();
            for (JsonNode node : (ArrayNode) data) {
                if (node.isObject()) result.add((ObjectNode) node);
            }
            return result;
        }
        if (data.isObject()) {
            JsonNode items = data.get("items");
            if (items != null && items.isArray()) {
                List<ObjectNode> result = new ArrayList<>();
                for (JsonNode node : (ArrayNode) items) {
                    if (node.isObject()) result.add((ObjectNode) node);
                }
                return result;
            }
        }
        return List.of();
    }

    public ObjectNode create(String name, Integer sizeBytes, Integer defaultTtlSeconds, Integer readTimeoutMs, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        if (sizeBytes != null) payload.put("sizeBytes", sizeBytes);
        if (defaultTtlSeconds != null) payload.put("defaultTTLSeconds", defaultTtlSeconds);
        if (readTimeoutMs != null) payload.put("readTimeoutMs", readTimeoutMs);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send("/api/cache", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode update(String name, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send("/api/cache/" + encodePath(name), "PATCH", headers, query, body, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public void delete(String name, Map<String, Object> query, Map<String, String> headers) {
        client.send("/api/cache/" + encodePath(name), "DELETE", headers, query, null, null, null, null, true);
    }

    public ObjectNode setEntry(String cache, String key, Object value, Integer ttlSeconds, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("value", value);
        if (ttlSeconds != null) payload.put("ttlSeconds", ttlSeconds);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            "/api/cache/" + encodePath(cache) + "/entries/" + encodePath(key),
            "PUT",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode getEntry(String cache, String key, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(
            "/api/cache/" + encodePath(cache) + "/entries/" + encodePath(key),
            "GET",
            headers,
            query,
            null,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode renewEntry(String cache, String key, Integer ttlSeconds, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        if (ttlSeconds != null) payload.put("ttlSeconds", ttlSeconds);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            "/api/cache/" + encodePath(cache) + "/entries/" + encodePath(key),
            "PATCH",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public void deleteEntry(String cache, String key, Map<String, Object> query, Map<String, String> headers) {
        client.send(
            "/api/cache/" + encodePath(cache) + "/entries/" + encodePath(key),
            "DELETE",
            headers,
            query,
            null,
            null,
            null,
            null,
            true
        );
    }
}
