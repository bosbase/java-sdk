package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;

public class SettingsService extends BaseService {
    public SettingsService(BosBase client) {
        super(client);
    }

    public ObjectNode getAll(Map<String, String> headers) {
        JsonNode data = client.send("/api/settings", "GET", headers, null, null, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode getCategory(String category, Map<String, String> headers) {
        ObjectNode all = getAll(headers);
        if (all == null) return null;
        JsonNode node = all.get(category);
        return node != null && node.isObject() ? (ObjectNode) node : null;
    }

    public ObjectNode update(Map<String, Object> body, Map<String, String> headers) {
        JsonNode data = client.send("/api/settings", "PATCH", headers, null, body, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public boolean testS3(String filesystem, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("filesystem", filesystem != null ? filesystem : "storage");
        client.send("/api/settings/test/s3", "POST", headers, null, payload, null, null, null, true);
        return true;
    }

    public boolean testEmail(String email, String template, String collectionIdOrName, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("template", template);
        if (collectionIdOrName != null && !collectionIdOrName.isBlank()) {
            payload.put("collection", collectionIdOrName);
        }
        client.send("/api/settings/test/email", "POST", headers, null, payload, null, null, null, true);
        return true;
    }

    public ObjectNode generateAppleClientSecret(String clientId, String teamId, String keyId, String privateKey, int duration, Map<String, String> headers) {
        Map<String, Object> payload = Map.of(
            "clientId", clientId,
            "teamId", teamId,
            "keyId", keyId,
            "privateKey", privateKey,
            "duration", duration
        );
        JsonNode data = client.send("/api/settings/apple/generate-client-secret", "POST", headers, null, payload, null, null, null, true);
        return data != null && data.isObject() ? (ObjectNode) data : null;
    }

    public ObjectNode updateMeta(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("meta", config), headers);
    }

    public ObjectNode updateTrustedProxy(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("trustedProxy", config), headers);
    }

    public ObjectNode updateRateLimits(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("rateLimits", config), headers);
    }

    public ObjectNode updateBatchSettings(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("batch", config), headers);
    }

    public ObjectNode updateSMTP(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("smtp", config), headers);
    }

    public ObjectNode getApplicationSettings(Map<String, String> headers) {
        ObjectNode all = getAll(headers);
        if (all == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("meta", all.get("meta"));
        result.put("trustedProxy", all.get("trustedProxy"));
        result.put("rateLimits", all.get("rateLimits"));
        result.put("batch", all.get("batch"));
        JsonNode node = JsonUtils.toJsonNode(result);
        return node != null && node.isObject() ? (ObjectNode) node : null;
    }

    public ObjectNode updateApplicationSettings(Map<String, Object> config, Map<String, String> headers) {
        return update(config, headers);
    }

    public ObjectNode getMailSettings(Map<String, String> headers) {
        ObjectNode all = getAll(headers);
        if (all == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("meta", all.get("meta"));
        result.put("smtp", all.get("smtp"));
        JsonNode node = JsonUtils.toJsonNode(result);
        return node != null && node.isObject() ? (ObjectNode) node : null;
    }

    public ObjectNode updateMailSettings(Map<String, Object> config, Map<String, String> headers) {
        return update(config, headers);
    }

    public ObjectNode updateS3(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("s3", config), headers);
    }

    public ObjectNode getStorageS3(Map<String, String> headers) {
        return getCategory("s3", headers);
    }

    public ObjectNode updateStorageS3(Map<String, Object> config, Map<String, String> headers) {
        return updateS3(config, headers);
    }

    public boolean testStorageS3(Map<String, String> headers) {
        return testS3("storage", headers);
    }

    public ObjectNode updateBackups(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("backups", config), headers);
    }

    public ObjectNode getBackupSettings(Map<String, String> headers) {
        return getCategory("backups", headers);
    }

    public ObjectNode updateBackupSettings(Map<String, Object> config, Map<String, String> headers) {
        return updateBackups(config, headers);
    }

    public ObjectNode setAutoBackupSchedule(String cron, Integer cronMaxKeep, Map<String, String> headers) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("cron", cron);
        if (cronMaxKeep != null) cfg.put("cronMaxKeep", cronMaxKeep);
        return updateBackups(cfg, headers);
    }

    public ObjectNode disableAutoBackup(Map<String, String> headers) {
        return updateBackups(Map.of("cron", ""), headers);
    }

    public boolean testBackupsS3(Map<String, String> headers) {
        return testS3("backups", headers);
    }

    public ObjectNode updateBatch(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("batch", config), headers);
    }

    public ObjectNode updateLogs(Map<String, Object> config, Map<String, String> headers) {
        return update(Map.of("logs", config), headers);
    }

    public ObjectNode getLogSettings(Map<String, String> headers) {
        return getCategory("logs", headers);
    }

    public ObjectNode updateLogSettings(Map<String, Object> config, Map<String, String> headers) {
        return updateLogs(config, headers);
    }

    public ObjectNode setLogRetentionDays(int maxDays, Map<String, String> headers) {
        return updateLogs(Map.of("maxDays", maxDays), headers);
    }

    public ObjectNode setMinLogLevel(int minLevel, Map<String, String> headers) {
        return updateLogs(Map.of("minLevel", minLevel), headers);
    }

    public ObjectNode setLogIPAddresses(boolean enabled, Map<String, String> headers) {
        return updateLogs(Map.of("logIP", enabled), headers);
    }

    public ObjectNode setLogAuthIds(boolean enabled, Map<String, String> headers) {
        return updateLogs(Map.of("logAuthId", enabled), headers);
    }
}
