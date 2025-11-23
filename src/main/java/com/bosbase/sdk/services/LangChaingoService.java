package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public class LangChaingoService extends BaseService {
    private final String basePath = "/api/langchaingo";

    public LangChaingoService(BosBase client) {
        super(client);
    }

    public ObjectNode completions(Map<String, Object> payload, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/completions", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode rag(Map<String, Object> payload, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/rag", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode queryDocuments(Map<String, Object> payload, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/documents/query", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode sql(Map<String, Object> payload, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/sql", "POST", headers, query, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }
}
