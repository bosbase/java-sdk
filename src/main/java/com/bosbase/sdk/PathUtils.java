package com.bosbase.sdk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PathUtils {
    private PathUtils() {}

    public static String encodePath(String segment) {
        try {
            return URLEncoder.encode(segment, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to encode path segment", e);
        }
    }
}
