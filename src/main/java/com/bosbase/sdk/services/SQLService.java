package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLService provides superuser-only SQL execution helpers.
 */
public class SQLService extends BaseService {
    private final String basePath = "/api/sql";

    public SQLService(BosBase client) {
        super(client);
    }

    /**
     * Execute a SQL statement and return the raw response.
     *
     * Only superusers can call this endpoint.
     */
    public ObjectNode execute(String query, Map<String, Object> body, Map<String, Object> queryParams, Map<String, String> headers) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", trimmed);
        if (body != null) payload.putAll(body);

        JsonNode data = client.send(basePath + "/execute", "POST", headers, queryParams, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }
}
