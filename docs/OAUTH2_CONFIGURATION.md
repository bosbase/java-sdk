# OAuth2 Configuration - Java SDK Documentation

Use the Records API helpers to integrate OAuth2 providers for your auth collections.

## Discover enabled providers

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

ObjectNode methods = pb.collection("users").listAuthMethods("oauth2", null, null, null);
System.out.println(methods);
```

## Start the OAuth2 flow

```java
pb.collection("users").authWithOAuth2(
    "google",
    url -> {
        // open browser or send the URL to your client
        System.out.println("Open OAuth2 URL: " + url);
    },
    List.of("profile", "email"),
    Map.of("name", "Demo User"), // optional create data
    null,
    null,
    null,
    null,
    null,
    120L,
    null
);
```

`authWithOAuth2` internally waits for the realtime callback on topic `@oauth2` and then exchanges the code with `authWithOAuth2Code`. If you already have the code and verifier you can call `authWithOAuth2Code` directly.
