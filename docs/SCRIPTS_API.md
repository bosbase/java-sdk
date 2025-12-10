# Scripts API - Java SDK Documentation

Superusers can manage stored scripts via `pb.scripts` and control their permissions through `pb.scriptsPermissions`.

## Manage scripts

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

// Create
pb.scripts.create("hello", "export default () => 'hi';", "Demo script", null, null, null);

// Update content
pb.scripts.update("hello", Map.of("content", "export default () => 'hi again';"), null, null);

// Execute stored script
ObjectNode result = pb.scripts.execute("hello", null, null);
System.out.println(result);
```

## Command helper

```java
pb.scripts.command("ls -la", null, null, null);
```

## Permissions

```java
// Create a permission manifest
pb.scriptsPermissions.create(
    null,
    "hello",
    "{ allow: true, match: 'auth' }",
    null,
    null,
    null
);

// Update permissions
pb.scriptsPermissions.update("hello", Map.of("content", "{ allow: false }"), null, null);
```

All script endpoints require an authenticated superuser; the SDK will throw if the current auth state is not superuser.
