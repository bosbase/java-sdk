# Custom Token Auth - Java SDK Documentation

BosBase supports binding a custom token (for example an external IdP token) to an auth record. The Java SDK exposes helpers on `RecordService`.

## Bind a token to an auth record

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Regular login to obtain a session
pb.collection("users").authWithPassword("user@example.com", "secret", null, null, null, null, null, null, null, null);

// Bind a custom token to the account
pb.collection("users").bindCustomToken(
    "user@example.com",
    "secret",
    "ext_token_123",
    null,
    null,
    null
);
```

## Unbind a token

```java
pb.collection("users").unbindCustomToken(
    "user@example.com",
    "secret",
    "ext_token_123",
    null,
    null,
    null
);
```

Binding stores the token server-side so future requests can be authenticated using it; unbinding removes that association.
