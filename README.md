# BosBase Java SDK

Java client for the BosBase API mirroring the JavaScript SDK surface. It wraps the HTTP endpoints exposed by the Go backend using OkHttp and Jackson so JVM projects can call collections, files, realtime, pub/sub, vectors, LLM documents, LangChaingo, caches, backups, settings, and batch operations just like the JS SDK.

## Quick Start

```java
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.services.RecordService;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Authenticate against an auth collection
pb.collection("users").authWithPassword("test@example.com", "123456", null, null, null, null, null, null,  null, null);

// List records with filter/expand
var posts = pb.collection("posts").getList(1, 10, false, "status = 'published'", "-created", "author", null, null, null);
posts.items.forEach(post -> System.out.println(post.get("title").asText()));

// Create a record
Map<String, Object> payload = new HashMap<>();
payload.put("title", "Hello Java!");
pb.collection("posts").create(payload, null, null, null);
```

## Installation

Gradle (Groovy):
```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.squareup.okhttp3:okhttp-sse:4.12.0'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.1'
```

Gradle (Kotlin DSL):
```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
```

See `build.gradle` for the full dependency set.

## Features

- `BosBase.send(...)` HTTP wrapper with beforeSend/afterSend hooks and auth/header injection
- `pb.collection("name")` exposes record CRUD, auth helpers (password, OTP, OAuth2), impersonation, and realtime subscriptions
- Batch requests via `pb.createBatch()`
- Services match the JS SDK: collections, files, logs, realtime, pubsub, health, backups, crons, vectors, LLM documents, LangChaingo, caches, settings, GraphQL
- Filter helper `pb.filter("title ~ {:title}", Map.of("title", "demo"))` with the same escaping rules as the JS SDK
- Multipart uploads using `FileAttachment` (single or multi-value fields)
- Auth stores: `BaseAuthStore`, `AuthStore` (memory), `LocalAuthStore` (Preferences), `AsyncAuthStore` (custom persistence)

## Building

From `java-sdk`:
```bash
./gradlew test       # if you have the Gradle wrapper available
# or
gradle test          # using a local Gradle installation
```

The project targets Java 11 and uses OkHttp for transport and Jackson for JSON handling.
