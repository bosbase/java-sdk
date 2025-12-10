# Realtime API - Java SDK Documentation

`pb.realtime` uses Server-Sent Events (SSE) to stream collection changes and custom topics.

## Subscribe to collection changes

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

Runnable unsub = pb.collection("posts").subscribe("*", event -> {
    System.out.println(event.get("action") + " -> " + event.get("record"));
}, null, null);
```

To watch a single record use `subscribe("RECORD_ID", ...)`.

## Unsubscribe

```java
// Remove a single listener
unsub.run();

// Remove everything for a topic or prefix
pb.realtime.unsubscribe("posts/*");
pb.realtime.unsubscribeByPrefix("posts");
```

The client automatically reconnects with exponential backoff and resubmits active subscriptions after a disconnect.
