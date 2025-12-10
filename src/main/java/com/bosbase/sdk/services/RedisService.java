package com.bosbase.sdk.services;

import static com.bosbase.sdk.PathUtils.encodePath;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisService exposes the Redis key APIs.
 */
public class RedisService extends BaseService {
    private final String basePath = "/api/redis";

    public RedisService(BosBase client) {
        super(client);
    }

    /**
     * Iterates redis keys using SCAN.
     */
    public ObjectNode listKeys(String cursor, String pattern, Integer count, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new HashMap<>();
        if (cursor != null) params.put("cursor", cursor);
        if (pattern != null) params.put("pattern", pattern);
        if (count != null) params.put("count", count);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(basePath + "/keys", "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Creates a new key only if it doesn't exist.
     */
    public ObjectNode createKey(String key, Object value, Integer ttlSeconds, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", key);
        payload.put("value", value);
        if (ttlSeconds != null) payload.put("ttlSeconds", ttlSeconds);
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath + "/keys", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Reads a key value.
     */
    public ObjectNode getKey(String key, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/keys/" + encodePath(key), "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Updates an existing key. If ttlSeconds is omitted the existing TTL is preserved.
     */
    public ObjectNode updateKey(String key, Object value, Integer ttlSeconds, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        if (value != null) payload.put("value", value);
        if (ttlSeconds != null) payload.put("ttlSeconds", ttlSeconds);
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath + "/keys/" + encodePath(key), "PUT", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Deletes a key.
     */
    public boolean deleteKey(String key, Map<String, Object> query, Map<String, String> headers) {
        client.send(basePath + "/keys/" + encodePath(key), "DELETE", headers, query, null, null, null, null, true);
        return true;
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }
}
