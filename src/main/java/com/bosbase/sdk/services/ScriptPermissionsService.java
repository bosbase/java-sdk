package com.bosbase.sdk.services;

import static com.bosbase.sdk.PathUtils.encodePath;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages per-script permission manifests.
 */
public class ScriptPermissionsService extends BaseService {
    private final String basePath = "/api/script-permissions";

    public ScriptPermissionsService(BosBase client) {
        super(client);
    }

    public ObjectNode create(String scriptId, String scriptName, String content, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String name = scriptName != null ? scriptName.trim() : "";
        String normalizedContent = content != null ? content.trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("scriptName is required");
        }
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("script_name", name);
        payload.put("content", normalizedContent);
        if (scriptId != null && !scriptId.trim().isEmpty()) {
            payload.put("script_id", scriptId.trim());
        }
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath, "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode get(String scriptName, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String name = scriptName != null ? scriptName.trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("scriptName is required");
        }

        JsonNode data = client.send(basePath + "/" + encodePath(name), "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode update(String scriptName, Map<String, Object> changes, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String name = scriptName != null ? scriptName.trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("scriptName is required");
        }

        Map<String, Object> payload = new HashMap<>();
        if (changes != null) {
            payload.putAll(changes);
        }
        if (payload.isEmpty()) {
            throw new IllegalArgumentException("at least one field must be provided");
        }

        JsonNode data = client.send(basePath + "/" + encodePath(name), "PATCH", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public boolean delete(String scriptName, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String name = scriptName != null ? scriptName.trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("scriptName is required");
        }

        client.send(basePath + "/" + encodePath(name), "DELETE", headers, query, null, null, null, null, true);
        return true;
    }

    private void requireSuperuser() {
        if (client.authStore == null || !client.authStore.isSuperuser()) {
            throw new IllegalStateException("Superuser authentication is required to manage script permissions");
        }
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }
}
