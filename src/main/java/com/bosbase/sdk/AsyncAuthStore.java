package com.bosbase.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Async persistence helper similar to JS AsyncAuthStore.
 */
public class AsyncAuthStore extends BaseAuthStore {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Consumer<String> saveFunc;
    private final Runnable clearFunc;

    public AsyncAuthStore(Consumer<String> saveFunc, Runnable clearFunc, Supplier<String> initial) {
        this.saveFunc = saveFunc;
        this.clearFunc = clearFunc;

        if (initial != null) {
            executor.execute(() -> {
                String payload = null;
                try {
                    payload = initial.get();
                } catch (Exception ignored) {
                }
                if (payload != null && !payload.isBlank()) {
                    try {
                        JsonNode parsed = mapper.readTree(payload);
                        String parsedToken = parsed.path("token").asText(null);
                        ObjectNode parsedModel = parsed.path("model").isObject()
                            ? (ObjectNode) parsed.path("model")
                            : parsed.path("record").isObject() ? (ObjectNode) parsed.path("record") : null;
                        if (parsedToken != null && !parsedToken.isBlank()) {
                            baseToken = parsedToken;
                            baseModel = parsedModel;
                            triggerChange();
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    @Override
    public void save(String newToken, ObjectNode newModel) {
        this.baseToken = newToken;
        this.baseModel = newModel;
        triggerChange();
        executor.execute(() -> {
            try {
                String serialized = mapper.writeValueAsString(java.util.Map.of("token", newToken, "model", newModel));
                saveFunc.accept(serialized);
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void clear() {
        this.baseToken = null;
        this.baseModel = null;
        triggerChange();
        executor.execute(() -> {
            try {
                if (clearFunc != null) {
                    clearFunc.run();
                } else {
                    saveFunc.accept("");
                }
            } catch (Exception ignored) {
            }
        });
    }
}
