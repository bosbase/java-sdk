package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosbase.sdk.PathUtils.encodePath;

public class LLMDocumentService extends BaseService {
    private final String basePath = "/api/llm-documents";

    public LLMDocumentService(BosBase client) {
        super(client);
    }

    private String collectionPath(String collection) {
        return basePath + "/" + encodePath(collection);
    }

    public List<ObjectNode> listCollections(Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(basePath + "/collections", "GET", headers, query, null, null, null, null, true);
        if (data != null && data.isArray()) {
            List<ObjectNode> list = new ArrayList<>();
            for (JsonNode node : (ArrayNode) data) {
                if (node.isObject()) list.add((ObjectNode) node);
            }
            return list;
        }
        return List.of();
    }

    public void createCollection(String name, Map<String, String> metadata, Map<String, Object> query, Map<String, String> headers) {
        client.send(
            basePath + "/collections/" + encodePath(name),
            "POST",
            headers,
            query,
            Map.of("metadata", metadata),
            null,
            null,
            null,
            true
        );
    }

    public void deleteCollection(String name, Map<String, Object> query, Map<String, String> headers) {
        client.send(basePath + "/collections/" + encodePath(name), "DELETE", headers, query, null, null, null, null, true);
    }

    public ObjectNode insert(String collection, Map<String, Object> document, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection), "POST", headers, query, document, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode get(String collection, String documentId, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/" + encodePath(documentId), "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode update(String collection, String documentId, Map<String, Object> document, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/" + encodePath(documentId), "PATCH", headers, query, document, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public void delete(String collection, String documentId, Map<String, Object> query, Map<String, String> headers) {
        client.send(collectionPath(collection) + "/" + encodePath(documentId), "DELETE", headers, query, null, null, null, null, true);
    }

    public ObjectNode list(String collection, Integer page, Integer perPage, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new java.util.HashMap<>();
        if (page != null) params.put("page", page);
        if (perPage != null) params.put("perPage", perPage);
        if (query != null) params.putAll(query);
        JsonNode data = client.send(collectionPath(collection), "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode query(String collection, Map<String, Object> options, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/documents/query", "POST", headers, query, options, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }
}
