package com.bosbase.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.prefs.Preferences;

/**
 * Persistent auth store backed by Java Preferences (desktop/server JVM).
 * Android callers can implement their own BaseAuthStore using SharedPreferences.
 */
public class LocalAuthStore extends BaseAuthStore {
    private final Preferences prefs;

    public LocalAuthStore() {
        this("bosbase.auth");
    }

    public LocalAuthStore(String namespace) {
        super();
        this.prefs = Preferences.userRoot().node(namespace);
    }

    @Override
    public String getToken() {
        return prefs.get("token", null);
    }

    @Override
    public ObjectNode getModel() {
        String raw = prefs.get("model", null);
        if (raw == null) return null;
        try {
            JsonNode node = mapper.readTree(raw);
            return node.isObject() ? (ObjectNode) node : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void save(String newToken, ObjectNode newModel) {
        prefs.put("token", newToken);
        if (newModel != null) {
            try {
                prefs.put("model", mapper.writeValueAsString(newModel));
            } catch (Exception ignored) {
            }
        } else {
            prefs.remove("model");
        }
        triggerChange();
    }

    @Override
    public void clear() {
        prefs.remove("token");
        prefs.remove("model");
        triggerChange();
    }
}
