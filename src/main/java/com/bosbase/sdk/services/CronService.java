package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosbase.sdk.PathUtils.encodePath;

public class CronService extends BaseService {
    public CronService(BosBase client) {
        super(client);
    }

    public List<ObjectNode> getFullList(Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send("/api/crons", "GET", headers, query, null, null, null, null, true);
        if (data != null && data.isArray()) {
            List<ObjectNode> list = new ArrayList<>();
            for (JsonNode node : (ArrayNode) data) {
                if (node.isObject()) {
                    list.add((ObjectNode) node);
                }
            }
            return list;
        }
        return List.of();
    }

    public void run(String jobId, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        client.send(
            "/api/crons/" + encodePath(jobId),
            "POST",
            headers,
            query,
            body,
            null,
            null,
            null,
            true
        );
    }
}
