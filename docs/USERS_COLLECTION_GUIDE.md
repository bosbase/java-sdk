# Users Collection Guide - Java SDK Documentation

BosBase ships with an auth collection for application users plus a dedicated `_superusers` collection for operators. The Java SDK offers shortcuts for both.

## Working with the default auth collection

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Sign up / login
pb.collection("users").authWithPassword("user@example.com", "secret", null, null, null, null, null, null, null, null);

// Current auth record
System.out.println(pb.authStore.getModel());
```

## Managing superusers

```java
// pb.admins() is an alias for the `_superusers` collection
pb.admins().create(Map.of(
    "email", "root@example.com",
    "password", "hunter2",
    "passwordConfirm", "hunter2"
), null, null, null);

boolean isSuperuser = pb.authStore.isSuperuser();
System.out.println("is superuser? " + isSuperuser);
```

Use superuser accounts for administrative tasks (settings, schema changes, SQL execution) and regular auth collections for end users.
