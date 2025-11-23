package com.bosbase.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class JsonUtils {
    private JsonUtils() {}

    public static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    static {
        // Load extra modules if available (Java time, etc.)
        MAPPER.findAndRegisterModules();
    }

    public static JsonNode toJsonNode(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }
        if (value instanceof byte[]) {
            return TextNode.valueOf(Base64.getEncoder().encodeToString((byte[]) value));
        }
        return MAPPER.valueToTree(value);
    }

    public static Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return Collections.emptyMap();
        }
        if (!node.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), jsonNodeToNative(entry.getValue())));
        return result;
    }

    public static Object toNative(JsonNode node) {
        return jsonNodeToNative(node);
    }

    private static Object jsonNodeToNative(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            // Prefer integer types when possible
            if (node.canConvertToInt()) {
                return node.intValue();
            }
            if (node.canConvertToLong()) {
                return node.longValue();
            }
            return node.decimalValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isArray()) {
            return JsonUtils.stream(node.elements())
                .map(JsonUtils::jsonNodeToNative)
                .collect(Collectors.toList());
        }
        if (node.isObject()) {
            return jsonNodeToMap(node);
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }

    private static <T> java.util.stream.Stream<T> stream(java.util.Iterator<T> it) {
        Iterable<T> iterable = () -> it;
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }
}
