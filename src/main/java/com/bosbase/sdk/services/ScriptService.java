package com.bosbase.sdk.services;

import static com.bosbase.sdk.PathUtils.encodePath;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScriptService manages stored server-side scripts.
 */
public class ScriptService extends BaseService {
    private final String basePath = "/api/scripts";

    public ScriptService(BosBase client) {
        super(client);
    }

    /**
    * Create a new script entry with version 1.
    */
    public ObjectNode create(String name, String content, String description, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmedName = name != null ? name.trim() : "";
        String trimmedContent = content != null ? content.trim() : "";
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("script name is required");
        }
        if (trimmedContent.isBlank()) {
            throw new IllegalArgumentException("script content is required");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", trimmedName);
        payload.put("content", trimmedContent);
        if (description != null) payload.put("description", description);
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath, "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Execute an arbitrary shell command in the functions directory.
     */
    public ObjectNode command(String command, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmed = command != null ? command.trim() : "";
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("command is required");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("command", trimmed);
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath + "/command", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Retrieve a script by its name.
     */
    public ObjectNode get(String name, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("script name is required");
        }

        JsonNode data = client.send(basePath + "/" + encodePath(trimmedName), "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * List all scripts.
     */
    public List<ObjectNode> list(Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        JsonNode data = client.send(basePath, "GET", headers, query, null, null, null, null, true);
        List<ObjectNode> items = new ArrayList<>();
        if (data != null && data.isObject()) {
            JsonNode arr = data.get("items");
            if (arr != null && arr.isArray()) {
                for (JsonNode node : (ArrayNode) arr) {
                    if (node.isObject()) {
                        items.add((ObjectNode) node);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Update an existing script and increment its version.
     */
    public ObjectNode update(String name, Map<String, Object> changes, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("script name is required");
        }
        boolean hasContent = changes != null && changes.containsKey("content");
        boolean hasDescription = changes != null && changes.containsKey("description");
        if (!hasContent && !hasDescription) {
            throw new IllegalArgumentException("at least one of content or description must be provided");
        }

        JsonNode data = client.send(basePath + "/" + encodePath(trimmedName), "PATCH", headers, query, changes, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Execute a stored script.
     */
    public ObjectNode execute(String name, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("script name is required");
        }

        JsonNode data = client.send(basePath + "/" + encodePath(trimmedName) + "/execute", "POST", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    /**
     * Delete a script by its name.
     */
    public boolean delete(String name, Map<String, Object> query, Map<String, String> headers) {
        requireSuperuser();

        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("script name is required");
        }

        client.send(basePath + "/" + encodePath(trimmedName), "DELETE", headers, query, null, null, null, null, true);
        return true;
    }

    private void requireSuperuser() {
        if (client.authStore == null || !client.authStore.isSuperuser()) {
            throw new IllegalStateException("Superuser authentication is required to manage scripts");
        }
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }
}
