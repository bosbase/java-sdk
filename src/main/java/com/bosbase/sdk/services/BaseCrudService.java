package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.ClientResponseError;
import com.bosbase.sdk.FileAttachment;
import com.bosbase.sdk.ResultList;
import com.bosbase.sdk.JsonUtils;
import com.bosbase.sdk.PathUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseCrudService extends BaseService {
    protected BaseCrudService(BosBase client) {
        super(client);
    }

    protected abstract String getBaseCrudPath();

    public ResultList<ObjectNode> getList(
        int page,
        int perPage,
        boolean skipTotal,
        String filter,
        String sort,
        String expand,
        String fields,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("perPage", perPage);
        params.put("skipTotal", skipTotal);
        if (filter != null) params.put("filter", filter);
        if (sort != null) params.put("sort", sort);
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(getBaseCrudPath(), "GET", headers, params, null, null, null, null, true);
        if (data == null || data.isNull()) {
            return new ResultList<>(page, perPage, 0, List.of(), null);
        }
        return asResultList(data);
    }

    public ResultList<ObjectNode> getList() {
        return getList(1, 30, false, null, null, null, null, null, null);
    }

    public List<ObjectNode> getFullList(
        int batch,
        String filter,
        String sort,
        String expand,
        String fields,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        List<ObjectNode> items = new ArrayList<>();
        int page = 1;
        while (true) {
            ResultList<ObjectNode> result = getList(page, batch, false, filter, sort, expand, fields, query, headers);
            items.addAll(result.items);
            if (items.size() >= result.totalItems || result.items.isEmpty()) break;
            page += 1;
        }
        return items;
    }

    public ObjectNode getOne(
        String id,
        String expand,
        String fields,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        Map<String, Object> params = new HashMap<>();
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(getBaseCrudPath() + "/" + PathUtils.encodePath(id), "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode getFirstListItem(
        String filter,
        String expand,
        String fields,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        ResultList<ObjectNode> result = getList(1, 1, true, filter, null, expand, fields, query, headers);
        ObjectNode first = result.items.isEmpty() ? null : result.items.get(0);
        if (first != null) return first;
        throw new ClientResponseError(
            client.buildUrl(getBaseCrudPath()).toString(),
            404,
            Map.of("code", 404, "message", "The requested resource wasn't found.", "data", Map.of()),
            false,
            null
        );
    }

    public ObjectNode create(
        Map<String, Object> body,
        Map<String, List<FileAttachment>> files,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        JsonNode data = client.send(getBaseCrudPath(), "POST", headers, query, body, files, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode update(
        String id,
        Map<String, Object> body,
        Map<String, List<FileAttachment>> files,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        JsonNode data = client.send(getBaseCrudPath() + "/" + id, "PATCH", headers, query, body, files, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public void delete(String id, Map<String, Object> query, Map<String, String> headers) {
        client.send(getBaseCrudPath() + "/" + id, "DELETE", headers, query, null, null, null, null, true);
    }

    private ResultList<ObjectNode> asResultList(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new ResultList<>(1, 0, 0, List.of(), node);
        }
        ObjectNode obj = (ObjectNode) node;
        int page = obj.path("page").isInt() ? obj.path("page").asInt() : 1;
        int perPage = obj.path("perPage").isInt() ? obj.path("perPage").asInt() : 0;
        int total = obj.has("totalItems") && obj.path("totalItems").isInt()
            ? obj.path("totalItems").asInt()
            : obj.path("total").isInt() ? obj.path("total").asInt() : 0;
        List<ObjectNode> items = new ArrayList<>();
        JsonNode arrNode = obj.path("items");
        if (arrNode.isArray()) {
            for (JsonNode item : (ArrayNode) arrNode) {
                if (item.isObject()) {
                    items.add((ObjectNode) item);
                }
            }
        }
        return new ResultList<>(page, perPage, total, items, obj);
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }
}
