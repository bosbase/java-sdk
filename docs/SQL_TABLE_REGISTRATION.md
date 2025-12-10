# SQL Table Registration - Java SDK Documentation

Use the collection helpers to register existing SQL tables as BosBase collections.

## Register existing tables

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

List<ObjectNode> collections = pb.collections.registerSqlTables(
    List.of("products", "orders"),
    Map.of("listRule", "@request.auth.id != ''"),
    null,
    null
);

collections.forEach(c -> System.out.println("Registered: " + c.path("name").asText()));
```

## Import with explicit definitions

```java
ObjectNode importResult = pb.collections.importSqlTables(
    List.of(
        Map.of("name", "products", "pk", "id"),
        Map.of("name", "orders", "pk", "id")
    ),
    Map.of("deleteMissing", false),
    null,
    null
);

System.out.println(importResult);
```

Registered tables behave like regular collections: you can query, filter, and expand relations using the familiar Records API.
