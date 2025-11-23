package com.bosbase.sdk;

public class BeforeSendResult {
    private final String url;
    private final RequestOptions options;

    public BeforeSendResult() {
        this(null, null);
    }

    public BeforeSendResult(String url, RequestOptions options) {
        this.url = url;
        this.options = options;
    }

    public String getUrl() {
        return url;
    }

    public RequestOptions getOptions() {
        return options;
    }
}
