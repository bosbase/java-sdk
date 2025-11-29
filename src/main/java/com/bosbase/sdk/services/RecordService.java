package com.bosbase.sdk.services;

import static com.bosbase.sdk.PathUtils.encodePath;

import com.bosbase.sdk.AuthStore;
import com.bosbase.sdk.BaseAuthStore;
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.ClientResponseError;
import com.bosbase.sdk.FileAttachment;
import com.bosbase.sdk.JsonUtils;
import com.bosbase.sdk.services.RealtimeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.HttpUrl;

public class RecordService extends BaseCrudService {
    private final String collectionIdOrName;

    private BaseAuthStore authStore() {
        return client.authStore;
    }

    private String baseCollectionPath() {
        return "/api/collections/" + encodePath(collectionIdOrName);
    }

    private boolean isSuperusers() {
        return "_superusers".equals(collectionIdOrName) || "_pbc_2773867675".equals(collectionIdOrName);
    }

    public RecordService(BosBase client, String collectionIdOrName) {
        super(client);
        this.collectionIdOrName = collectionIdOrName;
    }

    @Override
    protected String getBaseCrudPath() {
        return baseCollectionPath() + "/records";
    }

    public Runnable subscribe(String topic, java.util.function.Consumer<Map<String, Object>> callback, Map<String, Object> query, Map<String, String> headers) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Missing topic.");
        return client.realtime.subscribe(collectionIdOrName + "/" + topic, callback, query, headers);
    }

    public void unsubscribe(String topic) {
        if (topic != null) {
            client.realtime.unsubscribe(collectionIdOrName + "/" + topic);
        } else {
            client.realtime.unsubscribeByPrefix(collectionIdOrName);
        }
    }

    public int getCount(String filter, String expand, String fields, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new HashMap<>();
        if (filter != null) params.put("filter", filter);
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(getBaseCrudPath() + "/count", "GET", headers, params, null, null, null, null, true);
        return data != null && data.isObject() ? data.path("count").asInt(0) : 0;
    }

    public ObjectNode listAuthMethods(String fields, Map<String, Object> query, Map<String, String> headers, String requestKey) {
        Map<String, Object> params = new HashMap<>();
        if (fields != null) params.put("fields", fields);
        if (query != null) params.putAll(query);
        JsonNode data = client.send(
            baseCollectionPath() + "/auth-methods",
            "GET",
            headers,
            params,
            null,
            null,
            null,
            requestKey,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode authWithPassword(
        String identity,
        String password,
        String expand,
        String fields,
        String mfaId,
        Map<String, Object> body,
        Map<String, Object> query,
        Map<String, String> headers,
        Long autoRefreshThresholdSeconds,
        String requestKey
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("identity", identity);
        payload.put("password", password);
        if (body != null) payload.putAll(body);

        Map<String, Object> params = new HashMap<>();
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (mfaId != null) params.put("mfaId", mfaId);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(
            baseCollectionPath() + "/auth-with-password",
            "POST",
            headers,
            params,
            payload,
            null,
            null,
            requestKey,
            true
        );
        ObjectNode authData = authResponse(data);

        if (autoRefreshThresholdSeconds != null && isSuperusers()) {
            Map<String, Object> refreshQuery = new HashMap<>();
            refreshQuery.put("autoRefresh", true);
            if (query != null) refreshQuery.putAll(query);

            client.registerAutoRefresh(
                autoRefreshThresholdSeconds,
                () -> authRefresh(null, refreshQuery, headers),
                () -> {
                    Map<String, Object> reauthQuery = new HashMap<>();
                    reauthQuery.put("autoRefresh", true);
                    if (query != null) reauthQuery.putAll(query);
                    if (mfaId != null) reauthQuery.put("mfaId", mfaId);
                    authWithPassword(identity, password, expand, fields, mfaId, body, reauthQuery, headers, null, requestKey);
                }
            );
        }

        return authData;
    }

    public ObjectNode authRefresh(Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        JsonNode data = client.send(
            baseCollectionPath() + "/auth-refresh",
            "POST",
            headers,
            query,
            body != null ? body : Map.of(),
            null,
            null,
            null,
            true
        );
        return authResponse(data);
    }

    public ObjectNode requestOtp(String email, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            baseCollectionPath() + "/request-otp",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode authWithOtp(
        String otpId,
        String otp,
        String mfaId,
        Map<String, Object> query,
        Map<String, String> headers,
        String requestKey
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("otpId", otpId);
        payload.put("password", otp);
        Map<String, Object> params = new HashMap<>();
        if (mfaId != null) params.put("mfaId", mfaId);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(
            baseCollectionPath() + "/auth-with-otp",
            "POST",
            headers,
            params,
            payload,
            null,
            null,
            requestKey,
            true
        );
        return authResponse(data);
    }

    public boolean bindCustomToken(String email, String password, String token, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("token", token);
        if (body != null) payload.putAll(body);

        client.send(
            baseCollectionPath() + "/bind-token",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return true;
    }

    public boolean unbindCustomToken(String email, String password, String token, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("token", token);
        if (body != null) payload.putAll(body);

        client.send(
            baseCollectionPath() + "/unbind-token",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return true;
    }

    public ObjectNode authWithToken(
        String token,
        String expand,
        String fields,
        Map<String, Object> body,
        Map<String, Object> query,
        Map<String, String> headers,
        Long autoRefreshThresholdSeconds,
        String requestKey
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);
        if (body != null) payload.putAll(body);

        Map<String, Object> params = new HashMap<>();
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(
            baseCollectionPath() + "/auth-with-token",
            "POST",
            headers,
            params,
            payload,
            null,
            null,
            requestKey,
            true
        );
        ObjectNode authData = authResponse(data);

        if (autoRefreshThresholdSeconds != null && isSuperusers()) {
            Map<String, Object> refreshQuery = new HashMap<>();
            refreshQuery.put("autoRefresh", true);
            if (query != null) refreshQuery.putAll(query);

            client.registerAutoRefresh(
                autoRefreshThresholdSeconds,
                () -> authRefresh(null, refreshQuery, headers),
                () -> {
                    Map<String, Object> reauthQuery = new HashMap<>();
                    reauthQuery.put("autoRefresh", true);
                    if (query != null) reauthQuery.putAll(query);
                    authWithToken(token, expand, fields, body, reauthQuery, headers, null, requestKey);
                }
            );
        }

        return authData;
    }

    public ObjectNode authWithOAuth2Code(
        String provider,
        String code,
        String codeVerifier,
        String redirectURL,
        Map<String, Object> createData,
        String expand,
        String fields,
        Map<String, Object> body,
        Map<String, Object> query,
        Map<String, String> headers,
        String mfaId,
        Long timeoutSeconds,
        String requestKey
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("provider", provider);
        payload.put("code", code);
        payload.put("codeVerifier", codeVerifier);
        payload.put("redirectURL", redirectURL);
        if (createData != null) payload.put("createData", createData);
        if (body != null) payload.putAll(body);

        Map<String, Object> params = new HashMap<>();
        if (expand != null) params.put("expand", expand);
        if (fields != null) params.put("fields", fields);
        if (mfaId != null) params.put("mfaId", mfaId);
        if (query != null) params.putAll(query);

        JsonNode data = client.send(
            baseCollectionPath() + "/auth-with-oauth2",
            "POST",
            headers,
            params,
            payload,
            null,
            timeoutSeconds,
            requestKey,
            true
        );
        return authResponse(data);
    }

    public ObjectNode authWithOAuth2(
        String provider,
        java.util.function.Consumer<String> urlCallback,
        List<String> scopes,
        Map<String, Object> createData,
        String expand,
        String fields,
        Map<String, Object> query,
        Map<String, String> headers,
        String mfaId,
        Long timeoutSeconds,
        String requestKey
    ) {
        ObjectNode authMethods = listAuthMethods("mfa,otp,password,oauth2", query, headers, requestKey);
        OAuth2ProviderInfo providerInfo = findOAuthProvider(authMethods, provider);
        if (providerInfo == null) {
            throw new ClientResponseError(
                client.buildUrl(baseCollectionPath() + "/auth-methods").toString(),
                new IllegalArgumentException("Missing or invalid provider \"" + provider + "\".")
            );
        }

        RealtimeService realtime = new RealtimeService(client);
        final CompletableFuture<ObjectNode> result = new CompletableFuture<>();
        final java.util.concurrent.atomic.AtomicReference<Runnable> unsubscribeRef = new java.util.concurrent.atomic.AtomicReference<>();

        Runnable cleanup = () -> {
            Runnable unsub = unsubscribeRef.get();
            if (unsub != null) {
                try {
                    unsub.run();
                } catch (Exception ignored) {
                }
            }
            realtime.unsubscribe(null);
        };

        Runnable unsubscribe = realtime.subscribe("@oauth2", event -> {
            String state = asString(event.get("state"));
            String code = asString(event.get("code"));
            String error = asString(event.get("error"));
            try {
                if (state == null || state.isBlank() || !state.equals(realtime.getClientId())) {
                    throw new ClientResponseError(
                        client.buildUrl(baseCollectionPath() + "/auth-with-oauth2").toString(),
                        new IllegalStateException("State parameters don't match.")
                    );
                }

                if ((error != null && !error.isBlank()) || code == null || code.isBlank()) {
                    throw new ClientResponseError(
                        client.buildUrl(baseCollectionPath() + "/auth-with-oauth2").toString(),
                        new IllegalStateException("OAuth2 redirect error or missing code: " + (error != null ? error : ""))
                    );
                }

                Map<String, Object> params = new HashMap<>();
                if (query != null) params.putAll(query);
                if (mfaId != null) params.put("mfaId", mfaId);

                ObjectNode authData = authWithOAuth2Code(
                    providerInfo.name,
                    code,
                    providerInfo.codeVerifier,
                    client.buildUrl("/api/oauth2-redirect").toString(),
                    createData,
                    expand,
                    fields,
                    null,
                    params,
                    headers,
                    mfaId,
                    timeoutSeconds,
                    requestKey
                );

                result.complete(authData);
            } catch (Exception err) {
                result.completeExceptionally(err);
            } finally {
                cleanup.run();
            }
        }, null, null);
        unsubscribeRef.set(unsubscribe);

        if (!waitForRealtimeClientId(realtime, timeoutSeconds)) {
            cleanup.run();
            throw new ClientResponseError(
                client.buildUrl(baseCollectionPath() + "/auth-with-oauth2").toString(),
                new IllegalStateException("Failed to initialize realtime OAuth2 session.")
            );
        }

        String oauthUrl = buildOAuthUrl(providerInfo.authURL, realtime.getClientId(), scopes, client.buildUrl("/api/oauth2-redirect").toString());
        try {
            urlCallback.accept(oauthUrl);
        } catch (Exception err) {
            cleanup.run();
            throw err;
        }

        try {
            long waitFor = timeoutSeconds != null ? timeoutSeconds : 120L;
            return result.get(TimeUnit.SECONDS.toMillis(waitFor), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            throw new ClientResponseError(
                client.buildUrl(baseCollectionPath() + "/auth-with-oauth2").toString(),
                timeout
            );
        } catch (Exception err) {
            throw new ClientResponseError(
                client.buildUrl(baseCollectionPath() + "/auth-with-oauth2").toString(),
                err
            );
        } finally {
            cleanup.run();
        }
    }

    public ObjectNode requestPasswordReset(String email, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            baseCollectionPath() + "/request-password-reset",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode confirmPasswordReset(
        String token,
        String password,
        String passwordConfirm,
        Map<String, Object> body,
        Map<String, Object> query,
        Map<String, String> headers
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);
        payload.put("password", password);
        payload.put("passwordConfirm", passwordConfirm);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            baseCollectionPath() + "/confirm-password-reset",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return authResponse(data);
    }

    public ObjectNode requestVerification(String email, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            baseCollectionPath() + "/request-verification",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode confirmVerification(String token, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = Map.of("token", token);
        client.send(
            baseCollectionPath() + "/confirm-verification",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        markVerified(token);
        return emptyObject();
    }

    public ObjectNode requestEmailChange(String newEmail, String oldEmail, Map<String, Object> body, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("newEmail", newEmail);
        if (oldEmail != null) payload.put("oldEmail", oldEmail);
        if (body != null) payload.putAll(body);
        JsonNode data = client.send(
            baseCollectionPath() + "/request-email-change",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        return data != null && data.isObject() ? (ObjectNode) data : emptyObject();
    }

    public ObjectNode confirmEmailChange(String token, String password, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);
        if (password != null) payload.put("password", password);
        client.send(
            baseCollectionPath() + "/confirm-email-change",
            "POST",
            headers,
            query,
            payload,
            null,
            null,
            null,
            true
        );
        clearIfSameToken(token);
        return emptyObject();
    }

    public java.util.List<ObjectNode> listExternalAuths(String recordId, Map<String, Object> query, Map<String, String> headers) {
        String filter = client.filter("recordRef = {:recordId}", Map.of("recordId", recordId));
        Map<String, Object> params = new HashMap<>();
        if (query != null) params.putAll(query);
        return client.collection("_externalAuths").getFullList(200, filter, null, null, null, params, headers);
    }

    public boolean unlinkExternalAuth(String recordId, String provider, Map<String, Object> query, Map<String, String> headers) {
        String filter = client.filter("recordRef = {:recordId} && provider = {:provider}", Map.of("recordId", recordId, "provider", provider));
        Map<String, Object> params = new HashMap<>();
        if (query != null) params.putAll(query);
        ObjectNode externalAuth = client.collection("_externalAuths").getFirstListItem(filter, null, null, params, headers);
        client.collection("_externalAuths").delete(externalAuth.path("id").asText(), query, headers);
        return true;
    }

    public BosBase impersonate(String recordId, Integer durationSeconds, Map<String, Object> query, Map<String, String> headers) {
        Map<String, Object> params = new HashMap<>();
        if (durationSeconds != null) params.put("duration", durationSeconds);
        if (query != null) params.putAll(query);

        Map<String, String> headerMap = headers != null ? new HashMap<>(headers) : new HashMap<>();
        boolean hasAuthHeader = headerMap.keySet().stream().anyMatch(k -> "authorization".equalsIgnoreCase(k));
        if (!hasAuthHeader && client.authStore.getToken() != null) {
            headerMap.put("Authorization", client.authStore.getToken());
        }

        JsonNode data = client.send(
            baseCollectionPath() + "/impersonate/" + encodePath(recordId),
            "POST",
            headerMap,
            params,
            null,
            null,
            null,
            null,
            true
        );

        ObjectNode obj = data != null && data.isObject() ? (ObjectNode) data : emptyObject();
        String token = obj.path("token").asText(null);
        ObjectNode record = obj.path("record").isObject() ? (ObjectNode) obj.path("record") : null;
        BosBase impersonated = new BosBase(client.baseUrl, client.lang, new AuthStore(), client.httpClient);
        if (token != null) {
            impersonated.authStore.save(token, record);
        }
        return impersonated;
    }

    @Override
    public ObjectNode update(String id, Map<String, Object> body, Map<String, List<FileAttachment>> files, Map<String, Object> query, Map<String, String> headers) {
        ObjectNode item = super.update(id, body, files, query, headers);
        maybeUpdateAuthRecord(id, item);
        return item;
    }

    @Override
    public void delete(String id, Map<String, Object> query, Map<String, String> headers) {
        super.delete(id, query, headers);
        if (isAuthRecord(id)) {
            authStore().clear();
        }
    }

    private OAuth2ProviderInfo findOAuthProvider(ObjectNode authMethods, String providerName) {
        if (authMethods == null) return null;
        JsonNode oauth = authMethods.get("oauth2");
        if (oauth == null || !oauth.isObject()) return null;
        JsonNode providers = oauth.get("providers");
        if (providers == null || !providers.isArray()) return null;

        for (JsonNode provider : (ArrayNode) providers) {
            if (provider.isObject()) {
                String name = provider.path("name").asText(null);
                if (Objects.equals(name, providerName)) {
                    String authURL = provider.path("authURL").asText(null);
                    String codeVerifier = provider.path("codeVerifier").asText(null);
                    if (authURL != null && codeVerifier != null) {
                        return new OAuth2ProviderInfo(providerName, authURL, codeVerifier);
                    }
                }
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) return null;
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isTextual() || node.isNumber() || node.isBoolean()) {
                return node.asText();
            }
            return node.toString();
        }
        return value.toString().replace("\"", "");
    }

    private boolean waitForRealtimeClientId(RealtimeService realtime, Long timeoutSeconds) {
        long timeoutMs = TimeUnit.SECONDS.toMillis(timeoutSeconds != null ? timeoutSeconds : 15L);
        long start = System.currentTimeMillis();
        while (realtime.getClientId().isBlank() && System.currentTimeMillis() - start < timeoutMs) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignored) {
            }
        }
        return !realtime.getClientId().isBlank();
    }

    private String buildOAuthUrl(String authUrl, String state, List<String> scopes, String redirectUrl) {
        String combined = authUrl + redirectUrl;
        HttpUrl parsed = HttpUrl.parse(combined);
        if (parsed != null) {
            HttpUrl.Builder builder = parsed.newBuilder();
            builder.setQueryParameter("state", state);
            if (scopes != null && !scopes.isEmpty()) {
                builder.setQueryParameter("scope", String.join(" ", scopes));
            }
            return builder.build().toString();
        }

        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        String scopePart = (scopes == null || scopes.isEmpty())
            ? ""
            : "&scope=" + URLEncoder.encode(String.join(" ", scopes), StandardCharsets.UTF_8);
        String separator = combined.contains("?") ? "&" : "?";
        return combined + separator + "state=" + encodedState + scopePart;
    }

    private ObjectNode authResponse(JsonNode data) {
        ObjectNode obj = data != null && data.isObject() ? (ObjectNode) data : emptyObject();
        String token = obj.path("token").asText(null);
        ObjectNode record = obj.path("record").isObject() ? (ObjectNode) obj.path("record") : null;
        if (token != null) {
            authStore().save(token, record);
        }
        return obj;
    }

    private void markVerified(String verificationToken) {
        ObjectNode payload = decodeJwtPayload(verificationToken);
        ObjectNode model = authStore().getModel();
        if (payload == null || model == null) return;

        String payloadId = payload.path("id").asText(null);
        String payloadCollection = payload.path("collectionId").asText(null);
        String modelId = model.path("id").asText(null);
        String modelCollection = model.path("collectionId").asText(null);
        if (payloadId != null && payloadId.equals(modelId) && Objects.equals(payloadCollection, modelCollection)) {
            ObjectNode updated = model.deepCopy();
            updated.put("verified", true);
            authStore().save(authStore().getToken(), updated);
        }
    }

    private void clearIfSameToken(String verificationToken) {
        ObjectNode payload = decodeJwtPayload(verificationToken);
        ObjectNode model = authStore().getModel();
        if (payload == null || model == null) return;

        String payloadId = payload.path("id").asText(null);
        String payloadCollection = payload.path("collectionId").asText(null);
        String modelId = model.path("id").asText(null);
        String modelCollection = model.path("collectionId").asText(null);
        if (payloadId != null && payloadId.equals(modelId) && Objects.equals(payloadCollection, modelCollection)) {
            authStore().clear();
        }
    }

    private ObjectNode decodeJwtPayload(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode node = JsonUtils.MAPPER.readTree(decoded);
            return node != null && node.isObject() ? (ObjectNode) node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAuthRecord(String recordId) {
        ObjectNode model = authStore().getModel();
        String currentId = model != null ? model.path("id").asText(null) : null;
        return currentId != null && currentId.equals(recordId);
    }

    private void maybeUpdateAuthRecord(String recordId, ObjectNode newRecord) {
        if (isAuthRecord(recordId)) {
            String token = authStore().getToken();
            if (token != null) {
                authStore().save(token, newRecord);
            }
        }
    }

    private String padBase64(String raw) {
        int padding = (4 - raw.length() % 4) % 4;
        return raw + "=".repeat(padding);
    }

    private ObjectNode emptyObject() {
        return JsonUtils.MAPPER.createObjectNode();
    }

    private static class OAuth2ProviderInfo {
        final String name;
        final String authURL;
        final String codeVerifier;

        OAuth2ProviderInfo(String name, String authURL, String codeVerifier) {
            this.name = name;
            this.authURL = authURL;
            this.codeVerifier = codeVerifier;
        }
    }
}
