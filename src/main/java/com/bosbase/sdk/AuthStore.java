package com.bosbase.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * In-memory auth store (runtime only).
 */
public class AuthStore extends BaseAuthStore {
    public AuthStore() {
        super();
    }

    public AuthStore(String token, ObjectNode model) {
        super(token, model);
    }
}
