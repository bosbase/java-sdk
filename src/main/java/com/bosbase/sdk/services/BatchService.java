package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.FileAttachment;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bosbase.sdk.PathUtils.encodePath;

public class BatchService extends BaseService {
    private final List<QueuedRequest> requests = new ArrayList<>();
    private final Map<String, SubBatchService> collections = new HashMap<>();

    public BatchService(BosBase client) {
        super(client);
    }

    public SubBatchService collection(String collectionIdOrName) {
        return collections.computeIfAbsent(collectionIdOrName, key -> new SubBatchService(this, key));
    }

    public void queueRequest(String method, String url, Map<String, String> headers, Map<String, Object> body, Map<String, List<FileAttachment>> files) {
        Map<String, Object> jsonBody = new HashMap<>();
        Map<String, List<FileAttachment>> attachments = new HashMap<>();
        if (body != null) {
            Pair<Map<String, Object>, Map<String, List<FileAttachment>>> split = splitFiles(body);
            jsonBody.putAll(split.first);
            split.second.forEach((key, list) -> attachments.computeIfAbsent(key, __ -> new ArrayList<>()).addAll(list));
        }
        if (files != null) {
            files.forEach((key, list) -> attachments.computeIfAbsent(key, __ -> new ArrayList<>()).addAll(list));
        }
        requests.add(new QueuedRequest(method, url, headers != null ? headers : Map.of(), jsonBody, attachments));
    }

    public List<Object> send(Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        if (body != null) payload.putAll(body);

        List<Map<String, Object>> requestPayload = new ArrayList<>();
        for (QueuedRequest req : requests) {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("method", req.method);
            requestMap.put("url", req.url);
            requestMap.put("headers", req.headers);
            requestMap.put("body", req.body);
            requestPayload.add(requestMap);
        }
        payload.put("requests", requestPayload);

        Map<String, List<FileAttachment>> flatFiles = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            QueuedRequest req = requests.get(i);
            for (Map.Entry<String, List<FileAttachment>> entry : req.files.entrySet()) {
                String field = entry.getKey();
                List<FileAttachment> attachmentList = entry.getValue();
                for (int idx = 0; idx < attachmentList.size(); idx++) {
                    FileAttachment attachment = attachmentList.get(idx);
                    String fieldKey = attachmentList.size() > 1 ? field + "[" + idx + "]" : field;
                    flatFiles.computeIfAbsent("requests." + i + "." + fieldKey, __ -> new ArrayList<>()).add(attachment);
                }
            }
        }

        JsonNode data = client.send(
            "/api/batch",
            "POST",
            headers,
            query,
            payload,
            flatFiles.isEmpty() ? null : flatFiles,
            null,
            null,
            true
        );

        requests.clear();
        if (data != null && data.isArray()) {
            List<Object> results = new ArrayList<>();
            for (JsonNode node : data) {
                results.add(JsonUtils.toNative(node));
            }
            return results;
        }
        return List.of();
    }

    String buildRelative(String path, Map<String, Object> query) {
        okhttp3.HttpUrl url = client.buildUrl(path, query);
        String queryPart = url.encodedQuery() != null ? "?" + url.encodedQuery() : "";
        return url.encodedPath() + queryPart;
    }

    private Pair<Map<String, Object>, Map<String, List<FileAttachment>>> splitFiles(Map<String, Object> body) {
        Map<String, Object> jsonBody = new HashMap<>();
        Map<String, List<FileAttachment>> files = new HashMap<>();
        if (body == null) return new Pair<>(jsonBody, files);

        java.util.function.BiConsumer<String, FileAttachment> addAttachment = (key, attachment) ->
            files.computeIfAbsent(key, __ -> new ArrayList<>()).add(attachment);

        body.forEach((key, value) -> {
            if (value instanceof FileAttachment) {
                addAttachment.accept(key, (FileAttachment) value);
            } else if (value instanceof Iterable) {
                List<Object> items = new ArrayList<>();
                for (Object v : (Iterable<?>) value) {
                    items.add(v);
                }
                List<FileAttachment> attachments = items.stream().filter(v -> v instanceof FileAttachment).map(v -> (FileAttachment) v).collect(Collectors.toList());
                List<Object> regular = items.stream().filter(v -> !(v instanceof FileAttachment)).collect(Collectors.toList());
                if (attachments.size() == items.size() && !attachments.isEmpty()) {
                    attachments.forEach(att -> addAttachment.accept(key, att));
                } else {
                    jsonBody.put(key, regular);
                    if (!attachments.isEmpty()) {
                        String targetKey = (key.startsWith("+") || key.endsWith("+")) ? key : key + "+";
                        attachments.forEach(att -> addAttachment.accept(targetKey, att));
                    }
                }
            } else if (value != null && value.getClass().isArray()) {
                int len = java.lang.reflect.Array.getLength(value);
                List<Object> asList = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    asList.add(java.lang.reflect.Array.get(value, i));
                }
                List<FileAttachment> attachments = asList.stream().filter(v -> v instanceof FileAttachment).map(v -> (FileAttachment) v).collect(Collectors.toList());
                List<Object> regular = asList.stream().filter(v -> !(v instanceof FileAttachment)).collect(Collectors.toList());
                if (attachments.size() == asList.size() && !attachments.isEmpty()) {
                    attachments.forEach(att -> addAttachment.accept(key, att));
                } else {
                    jsonBody.put(key, regular);
                    if (!attachments.isEmpty()) {
                        String targetKey = (key.startsWith("+") || key.endsWith("+")) ? key : key + "+";
                        attachments.forEach(att -> addAttachment.accept(targetKey, att));
                    }
                }
            } else {
                jsonBody.put(key, value);
            }
        });

        return new Pair<>(jsonBody, files);
    }

    private static class QueuedRequest {
        final String method;
        final String url;
        final Map<String, String> headers;
        final Map<String, Object> body;
        final Map<String, List<FileAttachment>> files;

        QueuedRequest(String method, String url, Map<String, String> headers, Map<String, Object> body, Map<String, List<FileAttachment>> files) {
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
            this.files = files;
        }
    }

    public static class SubBatchService {
        private final BatchService batch;
        private final String collectionIdOrName;

        SubBatchService(BatchService batch, String collectionIdOrName) {
            this.batch = batch;
            this.collectionIdOrName = collectionIdOrName;
        }

        private String collectionUrl() {
            return "/api/collections/" + encodePath(collectionIdOrName) + "/records";
        }

        public void create(
            Map<String, Object> body,
            Map<String, Object> query,
            Map<String, List<FileAttachment>> files,
            Map<String, String> headers,
            String expand,
            String fields
        ) {
            Map<String, Object> params = new HashMap<>();
            if (expand != null) params.put("expand", expand);
            if (fields != null) params.put("fields", fields);
            if (query != null) params.putAll(query);
            batch.queueRequest("POST", buildUrl(collectionUrl(), params), headers != null ? headers : Map.of(), body, files);
        }

        public void upsert(
            Map<String, Object> body,
            Map<String, Object> query,
            Map<String, List<FileAttachment>> files,
            Map<String, String> headers,
            String expand,
            String fields
        ) {
            Map<String, Object> params = new HashMap<>();
            if (expand != null) params.put("expand", expand);
            if (fields != null) params.put("fields", fields);
            if (query != null) params.putAll(query);
            batch.queueRequest("PUT", buildUrl(collectionUrl(), params), headers != null ? headers : Map.of(), body, files);
        }

        public void update(
            String recordId,
            Map<String, Object> body,
            Map<String, Object> query,
            Map<String, List<FileAttachment>> files,
            Map<String, String> headers,
            String expand,
            String fields
        ) {
            Map<String, Object> params = new HashMap<>();
            if (expand != null) params.put("expand", expand);
            if (fields != null) params.put("fields", fields);
            if (query != null) params.putAll(query);
            String url = collectionUrl() + "/" + encodePath(recordId);
            batch.queueRequest("PATCH", buildUrl(url, params), headers != null ? headers : Map.of(), body, files);
        }

        public void delete(String recordId, Map<String, Object> query, Map<String, String> headers) {
            String url = collectionUrl() + "/" + encodePath(recordId);
            batch.queueRequest("DELETE", buildUrl(url, query), headers != null ? headers : Map.of(), null, null);
        }

        private String buildUrl(String path, Map<String, Object> query) {
            return batch.buildRelative(path, query);
        }
    }

    private static class Pair<F, S> {
        final F first;
        final S second;
        Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
}
