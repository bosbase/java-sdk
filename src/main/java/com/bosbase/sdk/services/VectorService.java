package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosbase.sdk.PathUtils.encodePath;

public class VectorService extends BaseService {
    private final String basePath = "/api/vectors";

    public VectorService(BosBase client) {
        super(client);
    }

    private String collectionPath(String collection) {
        if (collection == null || collection.isBlank()) {
            throw new IllegalArgumentException("collection must be provided for vector document operations");
        }
        return basePath + "/" + encodePath(collection);
    }

    public ObjectNode insert(Map<String, Object> document, String collection, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection), "POST", headers, query, document, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode batchInsert(Map<String, Object> options, String collection, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/documents/batch", "POST", headers, query, options, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode update(String documentId, Map<String, Object> document, String collection, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/" + encodePath(documentId), "PATCH", headers, query, document, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public void delete(String documentId, String collection, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        client.send(collectionPath(collection) + "/" + encodePath(documentId), "DELETE", headers, query, body, null, null, null, true);
    }

    public ObjectNode search(Map<String, Object> options, String collection, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/documents/search", "POST", headers, query, options, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode get(String documentId, String collection, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(collectionPath(collection) + "/" + encodePath(documentId), "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode list(String collection, Integer page, Integer perPage, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new java.util.HashMap<>();
        if (page != null) params.put("page", page);
        if (perPage != null) params.put("perPage", perPage);
        if (query != null) params.putAll(query);
        JsonNode data = client.send(collectionPath(collection), "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public void createCollection(String name, Map<String, Object> config, Map<String, Object> query, Map<String, String> headers) {
        client.send(basePath + "/collections/" + encodePath(name), "POST", headers, query, config, null, null, null, true);
    }

    public void updateCollection(String name, Map<String, Object> config, Map<String, Object> query, Map<String, String> headers) {
        client.send(basePath + "/collections/" + encodePath(name), "PATCH", headers, query, config, null, null, null, true);
    }

    public void deleteCollection(String name, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        client.send(basePath + "/collections/" + encodePath(name), "DELETE", headers, query, body, null, null, null, true);
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
}
