# Redis API - Java SDK Documentation

`pb.redis` wraps the Redis key endpoints so you can manage lightweight key/value data without leaving the SDK.

## List keys with SCAN

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

ObjectNode page = pb.redis.listKeys(null, "session:*", 100, null, null);
System.out.println("Cursor: " + page.path("cursor").asText());
System.out.println("Keys: " + page.path("items"));
```

## Create or update keys

```java
// Create if missing
pb.redis.createKey("session:42", Map.of("userId", 1), 300, null, null, null);

// Update value or TTL
pb.redis.updateKey("session:42", Map.of("userId", 1, "role", "admin"), 600, null, null, null);
```

## Read and delete

```java
ObjectNode value = pb.redis.getKey("session:42", null, null);
System.out.println(value);

pb.redis.deleteKey("session:42", null, null);
```

If `ttlSeconds` is omitted during updates, the existing TTL is preserved.
