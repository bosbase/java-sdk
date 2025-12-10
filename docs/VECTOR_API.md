# Vector API - Java SDK Documentation

`pb.vectors` provides a unified interface for inserting, searching, and managing vector collections.

## Insert and search

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Ensure collection exists
pb.vectors.createCollection("documents", Map.of("dimension", 384, "distance", "cosine"), null, null);

// Insert a document
pb.vectors.insert(
    Map.of(
        "vector", List.of(0.1, 0.2, 0.3),
        "content", "hello vectors",
        "metadata", Map.of("lang", "en")
    ),
    "documents",
    null,
    null
);

// Search by vector
ObjectNode results = pb.vectors.search(
    Map.of("queryVector", List.of(0.1, 0.2, 0.3), "limit", 5),
    "documents",
    null,
    null
);
System.out.println(results);
```

## Manage collections

```java
List<ObjectNode> collections = pb.vectors.listCollections(null, null);
collections.forEach(c -> System.out.println(c.path("name").asText()));

pb.vectors.updateCollection("documents", Map.of("distance", "l2"), null, null);
pb.vectors.deleteCollection("old_vectors", null, null, null);
```

Vector operations require a collection name; the SDK enforces it to avoid accidental cross-collection writes.
