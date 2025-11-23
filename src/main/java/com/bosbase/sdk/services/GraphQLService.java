package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

public class GraphQLService extends BaseService {
    public GraphQLService(BosBase client) {
        super(client);
    }

    public ObjectNode query(String query, Map<String, Object> variables, String operationName, Map<String, Object> queryParams, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("variables", variables != null ? variables : Map.of());
        if (operationName != null) {
            payload.put("operationName", operationName);
        }

        JsonNode response = client.send("/api/graphql", "POST", headers, queryParams, payload, null, null, null, true);
        return response != null && response.isObject() ? (ObjectNode) response : null;
    }
}
