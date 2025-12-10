# Management API - Java SDK Documentation

Superuser management endpoints are available through `pb.settings`, `pb.collections`, and `pb.admins()` helpers. Use them to bootstrap environments and enforce configuration.

## Update global settings

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

pb.settings.update(Map.of(
    "meta", Map.of("appName", "Demo App"),
    "logs", Map.of("maxDays", 14)
), null, null);
```

## Seed core collections

```java
// Create an auth collection and a view collection
pb.collections.createAuth("members", Map.of("listRule", "@request.auth.id != ''"), null);
pb.collections.createView("active_members", "SELECT * FROM members WHERE active = TRUE", null, null);
```

## Manage superusers

```java
// Use the built-in superuser collection through pb.admins()
pb.admins().create(Map.of(
    "email", "ops@example.com",
    "password", "strong-password",
    "passwordConfirm", "strong-password"
), null, null, null);
```

These APIs give operators full control over server configuration and schemas directly from Java code.
