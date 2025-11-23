package com.bosbase.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

@FunctionalInterface
public interface OnStoreChangeFunc {
    void accept(String token, ObjectNode model);
}
