package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

public class CollectionService extends BaseCrudService {
    public CollectionService(BosBase client) {
        super(client);
    }

    @Override
    protected String getBaseCrudPath() {
        return "/api/collections";
    }

    public ObjectNode getScaffolds(Map<String, String> headers) {
        JsonNode data = client.send(getBaseCrudPath() + "/meta/scaffolds", "GET", headers, null, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode createFromScaffold(String type, String name, Map<String, Object> overrides, Map<String, String> headers) {
        ObjectNode scaffolds = getScaffolds(headers);
        JsonNode scaffold = scaffolds.get(type);
        if (scaffold == null || !scaffold.isObject()) {
            throw new IllegalArgumentException("Scaffold for type \"" + type + "\" not found");
        }

        Map<String, Object> merged = JsonUtils.jsonNodeToMap(scaffold);
        merged.put("name", name);
        if (overrides != null) {
            deepMerge(merged, overrides);
        }

        return create(merged, null, null, headers);
    }

    public ObjectNode createBase(String name, Map<String, Object> overrides, Map<String, String> headers) {
        return createFromScaffold("base", name, overrides, headers);
    }

    public ObjectNode createAuth(String name, Map<String, Object> overrides, Map<String, String> headers) {
        return createFromScaffold("auth", name, overrides, headers);
    }

    public ObjectNode createView(String name, String viewQuery, Map<String, Object> overrides, Map<String, String> headers) {
        Map<String, Object> scaffoldOverrides = new HashMap<>();
        if (overrides != null) scaffoldOverrides.putAll(overrides);
        if (viewQuery != null) scaffoldOverrides.put("viewQuery", viewQuery);
        return createFromScaffold("view", name, scaffoldOverrides, headers);
    }

    public void truncate(String collectionIdOrName, Map<String, String> headers) {
        String encoded = URLEncoder.encode(collectionIdOrName, StandardCharsets.UTF_8);
        client.send(getBaseCrudPath() + "/" + encoded + "/truncate", "DELETE", headers, null, null, null, null, null, true);
    }

    public boolean deleteCollection(String collectionIdOrName, Map<String, String> headers) {
        delete(collectionIdOrName, null, headers);
        return true;
    }

    public List<ObjectNode> exportCollections(java.util.function.Predicate<ObjectNode> filterCollections, Map<String, String> headers) {
        List<ObjectNode> collections = getFullList(200, null, null, null, null, null, headers);
        List<ObjectNode> filtered = new ArrayList<>();
        for (ObjectNode coll : collections) {
            if (filterCollections == null || filterCollections.test(coll)) {
                filtered.add(coll);
            }
        }

        List<ObjectNode> result = new ArrayList<>();
        for (ObjectNode coll : filtered) {
            Map<String, Object> map = JsonUtils.jsonNodeToMap(coll);
            map.remove("created");
            map.remove("updated");
            JsonNode oauth = coll.get("oauth2");
            if (oauth != null && oauth.isObject()) {
                Map<String, Object> oauthMap = JsonUtils.jsonNodeToMap(oauth);
                oauthMap.remove("providers");
                map.put("oauth2", oauthMap);
            }
            JsonNode normalized = JsonUtils.toJsonNode(map);
            if (normalized.isObject()) {
                result.add((ObjectNode) normalized);
            }
        }
        return result;
    }

    public List<ObjectNode> normalizeForImport(List<ObjectNode> collections) {
        Set<String> seenIds = new HashSet<>();
        List<ObjectNode> normalized = new ArrayList<>();
        for (ObjectNode coll : collections) {
            String id = coll.path("id").asText(null);
            if (id != null && seenIds.contains(id)) {
                continue;
            }
            if (id != null) seenIds.add(id);
            normalized.add(normalizeCollection(coll));
        }
        return normalized;
    }

    public boolean importCollections(List<ObjectNode> collections, boolean deleteMissing, Map<String, String> headers) {
        client.send(
            getBaseCrudPath() + "/import",
            "PUT",
            headers,
            null,
            Map.of("collections", collections, "deleteMissing", deleteMissing),
            null,
            null,
            null,
            true
        );
        return true;
    }

    public ObjectNode addField(String collectionIdOrName, Map<String, Object> field, Map<String, String> headers) {
        String name = field.getOrDefault("name", "").toString();
        String type = field.getOrDefault("type", "").toString();
        if (name.isBlank()) throw new IllegalArgumentException("Field name is required");
        if (type.isBlank()) throw new IllegalArgumentException("Field type is required");

        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<ObjectNode> fields = toObjectList(collection.path("fields"));
        for (ObjectNode node : fields) {
            if (name.equals(node.path("name").asText())) {
                throw new IllegalArgumentException("Field with name \"" + name + "\" already exists");
            }
        }

        Map<String, Object> newFieldMap = new HashMap<>();
        newFieldMap.put("id", field.getOrDefault("id", ""));
        newFieldMap.put("name", name);
        newFieldMap.put("type", type);
        newFieldMap.put("system", field.getOrDefault("system", false));
        newFieldMap.put("hidden", field.getOrDefault("hidden", false));
        newFieldMap.put("presentable", field.getOrDefault("presentable", false));
        newFieldMap.put("required", field.getOrDefault("required", false));
        newFieldMap.putAll(field);
        JsonNode newFieldNode = JsonUtils.toJsonNode(newFieldMap);
        if (newFieldNode.isObject()) {
            fields.add((ObjectNode) newFieldNode);
        }

        Map<String, Object> updated = JsonUtils.jsonNodeToMap(collection);
        updated.put("fields", fields);
        return update(collectionIdOrName, updated, null, null, headers);
    }

    public ObjectNode updateField(String collectionIdOrName, String fieldName, Map<String, Object> updates, Map<String, String> headers) {
        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<ObjectNode> fields = toObjectList(collection.path("fields"));
        if (fields.isEmpty()) throw new IllegalArgumentException("Fields list is missing");

        int idx = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (fieldName.equals(fields.get(i).path("name").asText(null))) {
                idx = i;
                break;
            }
        }
        if (idx == -1) throw new IllegalArgumentException("Field with name \"" + fieldName + "\" not found");

        ObjectNode current = fields.get(idx);
        if (current.path("system").asBoolean(false) && (updates.containsKey("type") || updates.containsKey("name"))) {
            throw new IllegalArgumentException("Cannot modify system fields");
        }
        String newName = updates.containsKey("name") ? Objects.toString(updates.get("name"), null) : null;
        if (newName != null && !newName.equals(fieldName)) {
            for (ObjectNode node : fields) {
                if (newName.equals(node.path("name").asText(null))) {
                    throw new IllegalArgumentException("Field with name \"" + newName + "\" already exists");
                }
            }
        }

        Map<String, Object> merged = JsonUtils.jsonNodeToMap(current);
        merged.putAll(updates);
        JsonNode mergedNode = JsonUtils.toJsonNode(merged);
        if (mergedNode.isObject()) {
            fields.set(idx, (ObjectNode) mergedNode);
        }

        Map<String, Object> updated = JsonUtils.jsonNodeToMap(collection);
        updated.put("fields", fields);
        return update(collectionIdOrName, updated, null, null, headers);
    }

    public ObjectNode removeField(String collectionIdOrName, String fieldName, Map<String, String> headers) {
        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<ObjectNode> fields = toObjectList(collection.path("fields"));

        int idx = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (fieldName.equals(fields.get(i).path("name").asText(null))) {
                idx = i;
                break;
            }
        }
        if (idx == -1) throw new IllegalArgumentException("Field with name \"" + fieldName + "\" not found");
        if (fields.get(idx).path("system").asBoolean(false)) {
            throw new IllegalArgumentException("Cannot remove system fields");
        }
        fields.remove(idx);

        List<String> indexes = toIndexList(collection);
        indexes.removeIf(idxDef ->
            idxDef.contains("(" + fieldName + ")") ||
                idxDef.contains("(" + fieldName + ",") ||
                idxDef.contains(", " + fieldName + ")")
        );

        Map<String, Object> updated = JsonUtils.jsonNodeToMap(collection);
        updated.put("fields", fields);
        updated.put("indexes", indexes);
        return update(collectionIdOrName, updated, null, null, headers);
    }

    public ObjectNode getField(String collectionIdOrName, String fieldName, Map<String, String> headers) {
        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<ObjectNode> fields = toObjectList(collection.path("fields"));
        for (ObjectNode field : fields) {
            if (fieldName.equals(field.path("name").asText(null))) {
                return field;
            }
        }
        return null;
    }

    public ObjectNode addIndex(String collectionIdOrName, List<String> columns, boolean unique, String indexName, Map<String, String> headers) {
        if (columns == null || columns.isEmpty()) throw new IllegalArgumentException("At least one column must be specified");

        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<String> fieldNames = new ArrayList<>();
        for (ObjectNode field : toObjectList(collection.path("fields"))) {
            fieldNames.add(field.path("name").asText());
        }

        for (String column : columns) {
            if (!"id".equals(column) && !fieldNames.contains(column)) {
                throw new IllegalArgumentException("Field \"" + column + "\" does not exist in the collection");
            }
        }

        String collectionName = collection.path("name").asText(collectionIdOrName);
        String idxName = indexName != null ? indexName : "idx_" + collectionName + "_" + String.join("_", columns);
        String columnsStr = columns.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
        String definition = unique
            ? "CREATE UNIQUE INDEX `" + idxName + "` ON `" + collectionName + "` (" + columnsStr + ")"
            : "CREATE INDEX `" + idxName + "` ON `" + collectionName + "` (" + columnsStr + ")";

        List<String> indexes = toIndexList(collection);
        if (indexes.contains(definition)) {
            throw new IllegalArgumentException("Index already exists");
        }
        indexes.add(definition);

        Map<String, Object> updated = JsonUtils.jsonNodeToMap(collection);
        updated.put("indexes", indexes);
        return update(collectionIdOrName, updated, null, null, headers);
    }

    public ObjectNode removeIndex(String collectionIdOrName, List<String> columns, Map<String, String> headers) {
        if (columns == null || columns.isEmpty()) throw new IllegalArgumentException("At least one column must be specified");

        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        List<String> indexes = toIndexList(collection);
        int initialSize = indexes.size();

        indexes.removeIf(idx -> columns.stream().allMatch(column ->
            idx.contains("`" + column + "`") ||
                idx.contains("(" + column + ")") ||
                idx.contains("(" + column + ",") ||
                idx.contains(", " + column + ")")
        ));

        if (indexes.size() == initialSize) {
            throw new IllegalArgumentException("Index not found");
        }

        Map<String, Object> updated = JsonUtils.jsonNodeToMap(collection);
        updated.put("indexes", indexes);
        return update(collectionIdOrName, updated, null, null, headers);
    }

    public List<String> getIndexes(String collectionIdOrName, Map<String, String> headers) {
        ObjectNode collection = getOne(collectionIdOrName, null, null, null, headers);
        return toIndexList(collection);
    }

    public ObjectNode getSchema(String collectionIdOrName, Map<String, String> headers) {
        String encoded = URLEncoder.encode(collectionIdOrName, StandardCharsets.UTF_8);
        JsonNode result = client.send(getBaseCrudPath() + "/" + encoded + "/schema", "GET", headers, null, null, null, null, null, true);
        return result != null && result.isObject() ? (ObjectNode) result : null;
    }

    public ObjectNode getAllSchemas(Map<String, String> headers) {
        JsonNode result = client.send(getBaseCrudPath() + "/schemas", "GET", headers, null, null, null, null, null, true);
        return result != null && result.isObject() ? (ObjectNode) result : null;
    }

    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object current = target.get(key);
            if (current instanceof Map && value instanceof Map) {
                Map<String, Object> mergedChild = new HashMap<>();
                ((Map<?, ?>) current).forEach((k, v) -> {
                    if (k != null) mergedChild.put(k.toString(), v);
                });
                Map<String, Object> valueMap = new HashMap<>();
                ((Map<?, ?>) value).forEach((k, v) -> {
                    if (k != null) valueMap.put(k.toString(), v);
                });
                deepMerge(mergedChild, valueMap);
                target.put(key, mergedChild);
            } else {
                target.put(key, value);
            }
        }
    }

    private ObjectNode normalizeCollection(ObjectNode collection) {
        Map<String, Object> map = JsonUtils.jsonNodeToMap(collection);
        map.remove("created");
        map.remove("updated");

        List<ObjectNode> fields = toObjectList(collection.path("fields"));
        Set<String> seenIds = new HashSet<>();
        List<ObjectNode> deduped = new ArrayList<>();
        for (ObjectNode field : fields) {
            String id = field.path("id").asText(null);
            if (id != null && seenIds.contains(id)) {
                continue;
            }
            if (id != null) seenIds.add(id);
            deduped.add(field);
        }
        map.put("fields", deduped);

        JsonNode result = JsonUtils.toJsonNode(map);
        return result.isObject() ? (ObjectNode) result : collection;
    }

    private List<ObjectNode> toObjectList(JsonNode node) {
        List<ObjectNode> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : (ArrayNode) node) {
                if (item.isObject()) {
                    list.add((ObjectNode) item);
                }
            }
        }
        return list;
    }

    private List<String> toIndexList(ObjectNode node) {
        List<String> indexes = new ArrayList<>();
        JsonNode idxNode = node.get("indexes");
        if (idxNode != null && idxNode.isArray()) {
            for (JsonNode item : idxNode) {
                if (item.isTextual()) {
                    indexes.add(item.asText());
                }
            }
        }
        return indexes;
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }
}
