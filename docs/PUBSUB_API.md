# PubSub API - Java SDK Documentation

`pb.pubsub` provides WebSocket pub/sub messaging with acknowledgements and reconnection handling.

## Publish and subscribe

```java
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.services.PubSubService;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Subscribe
Runnable unsubscribe = pb.pubsub.subscribe("news", msg -> {
    System.out.println("[" + msg.topic + "] " + msg.data);
});

// Publish and wait for server ack
PubSubService.PublishAck ack = pb.pubsub.publish("news", Map.of("title", "Hello"));
System.out.println("Published as " + ack.id);

// Later
unsubscribe.run();
```

## Realtime helpers

```java
// Realtime-style envelope
pb.pubsub.realtimeSubscribe("chat", evt -> {
    System.out.println(evt.event + ": " + evt.payload);
});

pb.pubsub.realtimePublish("chat", "message", Map.of("text", "hi"), null);
```

The client auto-reconnects and re-subscribes when the socket is interrupted. Use `pb.pubsub.disconnect()` to close the socket and clear subscriptions.
