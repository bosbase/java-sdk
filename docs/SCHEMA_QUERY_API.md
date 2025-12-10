# Schema Query API - Java SDK Documentation

Use schema endpoints to introspect BosBase collections and SQL-backed tables. The Java SDK exposes them through `pb.collections`.

## Fetch a single collection schema

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

ObjectNode schema = pb.collections.getSchema("posts", null);
System.out.println(schema.path("fields"));
```

## Fetch all schemas

```java
ObjectNode schemas = pb.collections.getAllSchemas(null);
schemas.fields().forEachRemaining(entry -> {
    System.out.println(entry.getKey() + " => " + entry.getValue());
});
```

These endpoints are useful for tooling, migrations, and editor auto-complete because they provide the normalized field definitions and indexes for each collection.
