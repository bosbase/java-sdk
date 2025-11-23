package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogService extends BaseService {
    public LogService(BosBase client) {
        super(client);
    }

    public ObjectNode getList(int page, int perPage, String filter, String sort, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("perPage", perPage);
        if (filter != null) params.put("filter", filter);
        if (sort != null) params.put("sort", sort);
        if (query != null) params.putAll(query);
        JsonNode data = client.send("/api/logs", "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode getOne(String logId, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send("/api/logs/" + logId, "GET", headers, query, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public List<Object> getStats(Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send("/api/logs/stats", "GET", headers, query, null, null, null, null, true);
        if (data != null && data.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode node : (ArrayNode) data) {
                list.add(JsonUtils.toNative(node));
            }
            return list;
        }
        return List.of();
    }
}
