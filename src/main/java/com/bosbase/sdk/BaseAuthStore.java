package com.bosbase.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Base auth store with runtime memory storage and helper utilities.
 */
public class BaseAuthStore {
    public static final String DEFAULT_COOKIE_KEY = "pb_auth";

    protected final ObjectMapper mapper = JsonUtils.MAPPER;
    protected String baseToken;
    protected ObjectNode baseModel;
    private final CopyOnWriteArrayList<OnStoreChangeFunc> callbacks = new CopyOnWriteArrayList<>();

    public BaseAuthStore() {
        this(null, null);
    }

    public BaseAuthStore(String token, ObjectNode model) {
        this.baseToken = token;
        this.baseModel = model;
    }

    public String getToken() {
        return baseToken;
    }

    public ObjectNode getModel() {
        return baseModel;
    }

    public boolean isValid() {
        return !isTokenExpired(baseToken);
    }

    public void save(String newToken, ObjectNode newModel) {
        this.baseToken = newToken;
        this.baseModel = newModel;
        triggerChange();
    }

    public void clear() {
        this.baseToken = null;
        this.baseModel = null;
        triggerChange();
    }

    public Runnable onChange(OnStoreChangeFunc fn) {
        return onChange(fn, false);
    }

    public Runnable onChange(OnStoreChangeFunc fn, boolean fireImmediately) {
        callbacks.add(fn);
        if (fireImmediately) {
            fn.accept(getToken(), getModel());
        }
        return () -> callbacks.remove(fn);
    }

    public void loadFromCookie(String cookie) {
        loadFromCookie(cookie, DEFAULT_COOKIE_KEY);
    }

    public void loadFromCookie(String cookie, String key) {
        String rawValue = parseCookie(cookie).get(key);
        if (rawValue == null) return;
        try {
            JsonNode parsed = mapper.readTree(rawValue);
            String parsedToken = parsed.path("token").asText(null);
            ObjectNode parsedModel = parsed.path("model").isObject()
                ? (ObjectNode) parsed.path("model")
                : parsed.path("record").isObject() ? (ObjectNode) parsed.path("record") : null;
            if (parsedToken != null && !parsedToken.isBlank()) {
                save(parsedToken, parsedModel);
            }
        } catch (Exception ignored) {
        }
    }

    public String exportToCookie() {
        return exportToCookie(new CookieOptions(), DEFAULT_COOKIE_KEY);
    }

    public String exportToCookie(CookieOptions options, String key) {
        Date defaultExpires = getTokenExp(baseToken) != null
            ? Date.from(Instant.ofEpochSecond(getTokenExp(baseToken)))
            : new Date(0);
        CookieOptions finalOptions = options.expires == null ? options.withExpires(defaultExpires) : options;
        Map<String, Object> payload = Map.of(
            "token", baseToken,
            "model", baseModel,
            "record", baseModel
        );
        String serialized;
        try {
            serialized = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            serialized = "";
        }
        return serializeCookie(key, serialized, finalOptions);
    }

    protected void triggerChange() {
        for (OnStoreChangeFunc cb : callbacks) {
            try {
                cb.accept(getToken(), getModel());
            } catch (Exception ignored) {
            }
        }
    }

    protected static Map<String, String> parseCookie(String cookie) {
        return java.util.Arrays.stream(cookie.split(";"))
            .map(String::trim)
            .map(part -> {
                int idx = part.indexOf('=');
                if (idx <= 0) return null;
                String ckey = part.substring(0, idx).trim();
                String value = part.substring(idx + 1).trim();
                return Map.entry(ckey, value);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected static String serializeCookie(String key, String value, CookieOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(value);
        if (options.expires != null) {
            sb.append("; Expires=").append(options.expires);
        }
        if (options.path != null && !options.path.isBlank()) {
            sb.append("; Path=").append(options.path);
        }
        if (options.secure) sb.append("; Secure");
        if (options.httpOnly) sb.append("; HttpOnly");
        if (options.sameSite) sb.append("; SameSite=Strict");
        return sb.toString();
    }

    protected static boolean isTokenExpired(String token) {
        Long exp = getTokenExp(token);
        if (exp == null) return true;
        long now = System.currentTimeMillis() / 1000;
        return now >= exp;
    }

    protected static Long getTokenExp(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode node = JsonUtils.MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
            return node.path("exp").isNumber() ? node.path("exp").asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected static String padBase64(String raw) {
        int padding = (4 - raw.length() % 4) % 4;
        return raw + "=".repeat(padding);
    }
}
