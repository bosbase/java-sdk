package com.bosbase.sdk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable request options passed to hooks so callers can tweak outgoing requests.
 */
public class RequestOptions {
    public String method = "GET";
    public Map<String, String> headers = new HashMap<>();
    public Map<String, Object> query = new HashMap<>();
    public Object body;
    public Map<String, List<FileAttachment>> files;
    public Long timeoutSeconds;
    public String requestKey;
    public boolean autoCancel = true;

    public RequestOptions() {}

    public RequestOptions(RequestOptions other) {
        if (other != null) {
            this.method = other.method;
            this.headers = new HashMap<>(other.headers);
            this.query = new HashMap<>(other.query);
            this.body = other.body;
            this.files = other.files;
            this.timeoutSeconds = other.timeoutSeconds;
            this.requestKey = other.requestKey;
            this.autoCancel = other.autoCancel;
        }
    }
}
