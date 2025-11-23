package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.FileAttachment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map;

import static com.bosbase.sdk.PathUtils.encodePath;

public class BackupService extends BaseService {
    public BackupService(BosBase client) {
        super(client);
    }

    public List<ObjectNode> getFullList(Map<String, String> headers) {
        JsonNode data = client.send("/api/backups", "GET", headers, null, null, null, null, null, true);
        if (data != null && data.isArray()) {
            List<ObjectNode> list = new ArrayList<>();
            for (JsonNode node : (ArrayNode) data) {
                if (node.isObject()) list.add((ObjectNode) node);
            }
            return list;
        }
        return List.of();
    }

    public boolean create(String name, Map<String, String> headers) {
        client.send("/api/backups", "POST", headers, null, Map.of("name", name), null, null, null, true);
        return true;
    }

    public boolean upload(Map<String, Object> body, Map<String, String> headers) {
        client.send("/api/backups/upload", "POST", headers, null, body, null, null, null, true);
        return true;
    }

    public boolean upload(Map<String, Object> body, Map<String, List<FileAttachment>> files, Map<String, String> headers) {
        client.send("/api/backups/upload", "POST", headers, null, body, files, null, null, true);
        return true;
    }

    public boolean delete(String key, Map<String, String> headers) {
        client.send("/api/backups/" + encodePath(key), "DELETE", headers, null, null, null, null, null, true);
        return true;
    }

    public boolean restore(String key, Map<String, String> headers) {
        client.send("/api/backups/" + encodePath(key) + "/restore", "POST", headers, null, null, null, null, null, true);
        return true;
    }

    public String getDownloadURL(String token, String key) {
        return client.buildUrl("/api/backups/" + encodePath(key), Map.of("token", token)).toString();
    }
}
