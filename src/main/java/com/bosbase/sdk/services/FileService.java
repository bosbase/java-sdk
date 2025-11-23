package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FileService extends BaseService {
    public FileService(BosBase client) {
        super(client);
    }

    public String getURL(ObjectNode record, String filename, String thumb, String token, Boolean download, Map<String, Object> query) {
        if (filename == null || filename.isBlank()) return "";
        if (record == null) return "";

        String recordId = record.path("id").asText(null);
        String collection = record.has("collectionId")
            ? record.path("collectionId").asText(null)
            : record.path("collectionName").asText(null);
        if (recordId == null || collection == null) return "";

        String[] parts = new String[] {
            "api",
            "files",
            urlEncode(collection),
            urlEncode(recordId),
            urlEncode(filename)
        };

        Map<String, Object> params = new HashMap<>();
        if (thumb != null) params.put("thumb", thumb);
        if (token != null) params.put("token", token);
        if (download != null && download) params.put("download", "1");
        if (query != null) params.putAll(query);

        return client.buildUrl(String.join("/", parts), params).toString();
    }

    public String getToken(Map<String, String> headers) {
        JsonNode data = client.send("/api/files/token", "POST", headers, null, null, null, null, null, true);
        return data != null && data.isObject() ? data.path("token").asText("") : "";
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
