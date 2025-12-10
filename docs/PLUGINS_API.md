# Plugins API - Java SDK Documentation

Plugins are proxied through the Java SDK via `pb.plugins`. You can send HTTP requests, open SSE streams, or connect WebSockets to your plugin endpoints.

## HTTP plugin calls

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.JsonNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

JsonNode result = pb.plugins.request(
    "POST",
    "/hello",
    Map.of("X-Plugin-Header", "demo"),
    Map.of("debug", true),
    Map.of("name", "Ada"),
    null,
    null,
    null
);
System.out.println(result);
```

`request` automatically prefixes the path with `/api/plugins` and forwards auth headers.

## SSE streams

```java
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

EventSource stream = pb.plugins.sse("/events", new EventSourceListener() {
    @Override public void onEvent(EventSource source, String id, String type, String data) {
        System.out.println("event: " + type + " -> " + data);
    }
}, Map.of(), Map.of());
```

## WebSockets

```java
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

WebSocket socket = pb.plugins.websocket("/chat", new WebSocketListener() {
    @Override public void onMessage(WebSocket webSocket, String text) {
        System.out.println("ws: " + text);
    }
}, Map.of(), Map.of());
```

The SDK appends your auth token as a `token` query parameter for SSE/WebSocket calls to simplify authentication.
