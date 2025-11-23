package com.bosbase.sdk;

import java.util.Map;

/**
 * Normalized HTTP error matching the JS SDK shape.
 */
public class ClientResponseError extends RuntimeException {
    private final String url;
    private final Integer status;
    private final Map<String, Object> response;
    private final boolean isAbort;
    private final Throwable originalError;

    public ClientResponseError(String url, Integer status, Map<String, Object> response, boolean isAbort, Throwable originalError) {
        super(buildMessage(url, status, response), originalError);
        this.url = url;
        this.status = status;
        this.response = response;
        this.isAbort = isAbort;
        this.originalError = originalError;
    }

    public ClientResponseError(String url, Throwable originalError) {
        this(url, null, java.util.Collections.emptyMap(), false, originalError);
    }

    public ClientResponseError(Throwable originalError) {
        this("", null, java.util.Collections.emptyMap(), false, originalError);
    }

    public String getUrl() {
        return url;
    }

    public Integer getStatus() {
        return status;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public boolean isAbort() {
        return isAbort;
    }

    public Throwable getOriginalError() {
        return originalError;
    }

    private static String buildMessage(String url, Integer status, Map<String, Object> response) {
        String statusText = status != null ? status.toString() : "n/a";
        String message = response != null && response.get("message") != null
            ? response.get("message").toString()
            : "HTTP " + statusText;
        return message + " (" + url + ")";
    }
}
