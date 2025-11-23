package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public class HealthService extends BaseService {
    public HealthService(BosBase client) {
        super(client);
    }

    public ObjectNode check(Map<String, String> headers) {
        JsonNode data = client.send("/api/health", "GET", headers, null, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }
}
