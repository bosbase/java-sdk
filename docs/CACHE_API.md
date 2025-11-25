# Cache API - Java SDK Documentation

BosBase caches combine in-memory [FreeCache](https://github.com/coocood/freecache) storage with persistent database copies. Each cache instance is safe to use in single-node or multi-node (cluster) mode: nodes read from FreeCache first, fall back to the database if an item is missing or expired, and then reload FreeCache automatically.

The Java SDK exposes the cache endpoints through `pb.caches`. Typical use cases include:

- Caching AI prompts/responses that must survive restarts.
- Quickly sharing feature flags and configuration between workers.
- Preloading expensive vector search results for short periods.

> **Timeouts & TTLs:** Each cache defines a default TTL (in seconds). Individual entries may provide their own `ttlSeconds`. A value of `0` keeps the entry until it is manually deleted.

## List available caches

The `list()` function allows you to query and retrieve all currently available caches, including their names and capacities. This is particularly useful for AI systems to discover existing caches before creating new ones, avoiding duplicate cache creation.

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

// Query all available caches
List<ObjectNode> caches = pb.caches.list(null);

// Each cache object contains:
// - name: string - The cache identifier
// - sizeBytes: number - The cache capacity in bytes
// - defaultTTLSeconds: number - Default expiration time
// - readTimeoutMs: number - Read timeout in milliseconds
// - created: string - Creation timestamp (RFC3339)
// - updated: string - Last update timestamp (RFC3339)

// Example: Find a cache by name and check its capacity
Optional<ObjectNode> targetCache = caches.stream()
    .filter(c -> "ai-session".equals(c.path("name").asText()))
    .findFirst();

if (targetCache.isPresent()) {
    ObjectNode cache = targetCache.get();
    System.out.println("Cache \"" + cache.path("name").asText() + 
                      "\" has capacity of " + cache.path("sizeBytes").asLong() + " bytes");
    // Use the existing cache directly
} else {
    System.out.println("Cache not found, create a new one if needed");
}
```

## Manage cache configurations

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

// List all available caches (including name and capacity).
// This is useful for AI to discover existing caches before creating new ones.
List<ObjectNode> caches = pb.caches.list(null);
System.out.println("Available caches: " + caches);

// Find an existing cache by name
Optional<ObjectNode> existingCache = caches.stream()
    .filter(c -> "ai-session".equals(c.path("name").asText()))
    .findFirst();

if (existingCache.isPresent()) {
    ObjectNode cache = existingCache.get();
    System.out.println("Found cache \"" + cache.path("name").asText() + 
                      "\" with capacity " + cache.path("sizeBytes").asLong() + " bytes");
    // Use the existing cache directly without creating a new one
} else {
    // Create a new cache only if it doesn't exist
    Map<String, Object> cacheConfig = new HashMap<>();
    cacheConfig.put("name", "ai-session");
    cacheConfig.put("sizeBytes", 64 * 1024 * 1024);
    cacheConfig.put("defaultTTLSeconds", 300);
    cacheConfig.put("readTimeoutMs", 25); // optional concurrency guard
    pb.caches.create(cacheConfig, null);
}

// Update limits later (eg. shrink TTL to 2 minutes).
Map<String, Object> updateConfig = new HashMap<>();
updateConfig.put("defaultTTLSeconds", 120);
pb.caches.update("ai-session", updateConfig, null);

// Delete the cache (DB rows + FreeCache).
pb.caches.delete("ai-session", null);
```

Field reference:

| Field | Description |
|-------|-------------|
| `sizeBytes` | Approximate FreeCache size. Values too small (<512KB) or too large (>512MB) are clamped. |
| `defaultTTLSeconds` | Default expiration for entries. `0` means no expiration. |
| `readTimeoutMs` | Optional lock timeout while reading FreeCache. When exceeded, the value is fetched from the database instead. |

## Work with cache entries

```java
// Store an object in cache. The same payload is serialized into the DB.
Map<String, Object> entryData = new HashMap<>();
entryData.put("prompt", "describe Saturn");
entryData.put("embedding", List.of(/* vector */));

Map<String, Object> setOptions = new HashMap<>();
setOptions.put("ttlSeconds", 90); // per-entry TTL in seconds

pb.caches.setEntry("ai-session", "dialog:42", entryData, setOptions, null);

// Read from cache. `source` indicates where the hit came from.
ObjectNode entry = pb.caches.getEntry("ai-session", "dialog:42", null);

System.out.println(entry.path("source").asText());   // "cache" or "database"
if (entry.has("expiresAt")) {
    System.out.println(entry.path("expiresAt").asText()); // RFC3339 timestamp
}

// Renew an entry's TTL without changing its value.
// This extends the expiration time by the specified TTL (or uses the cache's default TTL if omitted).
Map<String, Object> renewOptions = new HashMap<>();
renewOptions.put("ttlSeconds", 120); // extend by 120 seconds
ObjectNode renewed = pb.caches.renewEntry("ai-session", "dialog:42", renewOptions, null);
if (renewed.has("expiresAt")) {
    System.out.println(renewed.path("expiresAt").asText()); // new expiration time
}

// Delete an entry.
pb.caches.deleteEntry("ai-session", "dialog:42", null);
```

### Cluster-aware behaviour

1. **Write-through persistence** – every `setEntry` writes to FreeCache and the `_cache_entries` table so other nodes (or a restarted node) can immediately reload values.
2. **Read path** – FreeCache is consulted first. If a lock cannot be acquired within `readTimeoutMs` or if the entry is missing/expired, BosBase queries the database copy and repopulates FreeCache in the background.
3. **Automatic cleanup** – expired entries are ignored and removed from the database when fetched, preventing stale data across nodes.

Use caches whenever you need fast, transient data that must still be recoverable or shareable across BosBase nodes.

