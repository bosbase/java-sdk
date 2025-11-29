# Authentication - Java SDK Documentation

## Overview

Authentication in BosBase is stateless and token-based. A client is considered authenticated as long as it sends a valid `Authorization: YOUR_AUTH_TOKEN` header with requests.

**Key Points:**
- **No sessions**: BosBase APIs are fully stateless (tokens are not stored in the database)
- **No logout endpoint**: To "logout", simply clear the token from your local state (`pb.authStore.clear()`)
- **Token generation**: Auth tokens are generated through auth collection Web APIs or programmatically
- **Admin users**: `_superusers` collection works like regular auth collections but with full access (API rules are ignored)
- **OAuth2 limitation**: OAuth2 is not supported for `_superusers` collection

## Authentication Methods

BosBase supports multiple authentication methods that can be configured individually for each auth collection:

1. **Password Authentication** - Email/username + password
2. **OTP Authentication** - One-time password via email
3. **OAuth2 Authentication** - Google, GitHub, Microsoft, etc.
4. **Multi-factor Authentication (MFA)** - Requires 2 different auth methods

## Authentication Store

The SDK maintains an `authStore` that automatically manages the authentication state:

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://localhost:8090");

// Check authentication status
System.out.println(pb.authStore.isValid());      // true/false
System.out.println(pb.authStore.getToken());        // current auth token
System.out.println(pb.authStore.getModel());       // authenticated user record

// Clear authentication (logout)
pb.authStore.clear();
```

## Password Authentication

Authenticate using email/username and password. The identity field can be configured in the collection options (default is email).

**Backend Endpoint:** `POST /api/collections/{collection}/auth-with-password`

### Basic Usage

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://localhost:8090");

// Authenticate with email and password
ObjectNode authData = pb.collection("users").authWithPassword(
    "test@example.com",
    "password123",
    null, null, null, null, null, null, null, null
);

// Auth data is automatically stored in pb.authStore
System.out.println(pb.authStore.isValid());  // true
System.out.println(pb.authStore.getToken());    // JWT token
System.out.println(pb.authStore.getModel().path("id").asText()); // user record ID
```

### Response Format

```java
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "record": {
    "id": "record_id",
    "email": "test@example.com",
    // ... other user fields
  }
}
```

### Error Handling with MFA

```java
try {
    pb.collection("users").authWithPassword("test@example.com", "pass123", null, null, null, null, null, null, null, null);
} catch (ClientResponseError err) {
    // Check for MFA requirement
    if (err.response != null && err.response.containsKey("mfaId")) {
        String mfaId = err.response.get("mfaId").toString();
        // Handle MFA flow (see Multi-factor Authentication section)
    } else {
        System.err.println("Authentication failed: " + err);
    }
}
```

## OTP Authentication

One-time password authentication via email.

**Backend Endpoints:**
- `POST /api/collections/{collection}/request-otp` - Request OTP
- `POST /api/collections/{collection}/auth-with-otp` - Authenticate with OTP

### Request OTP

```java
// Send OTP to user's email
ObjectNode result = pb.collection("users").requestOTP("test@example.com", null, null, null);
System.out.println(result.path("otpId").asText());  // OTP ID to use in authWithOTP
```

### Authenticate with OTP

```java
// Step 1: Request OTP
ObjectNode result = pb.collection("users").requestOTP("test@example.com", null, null, null);

// Step 2: User enters OTP from email
ObjectNode authData = pb.collection("users").authWithOTP(
    result.path("otpId").asText(),
    "123456",  // OTP code from email (sent as `password` to match the API)
    null, null, null, null, null, null, null, null
);
```

## Custom Token Authentication

Bind a custom token to an auth record and reuse it later for signing in without a password.

**Backend Endpoints:**
- `POST /api/collections/{collection}/bind-token`
- `POST /api/collections/{collection}/unbind-token`
- `POST /api/collections/{collection}/auth-with-token`

```java
// Bind a custom token after verifying email/password
pb.collection("users").bindCustomToken("test@example.com", "password123", "my-custom-token", null, null, null);

// Authenticate with the previously bound token
pb.collection("users").authWithToken("my-custom-token", null, null, null, null, null, null, null);

// Remove the binding if needed
pb.collection("users").unbindCustomToken("test@example.com", "password123", "my-custom-token", null, null, null);
```

## OAuth2 Authentication

**Backend Endpoint:** `POST /api/collections/{collection}/auth-with-oauth2`

### All-in-One Method (Recommended)

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("https://bosbase.io");

// Opens popup window with OAuth2 provider page
ObjectNode authData = pb.collection("users").authWithOAuth2(
    "google",
    null, null, null, null, null, null, null, null, null, null, null, null
);

System.out.println(pb.authStore.getToken());
System.out.println(pb.authStore.getModel());
```

### Manual Code Exchange

```java
// Get auth methods
ObjectNode authMethods = pb.collection("users").listAuthMethods(null, null, null, null);
ArrayNode providers = (ArrayNode) authMethods.path("oauth2").path("providers");

// Exchange code for token (after OAuth2 redirect)
ObjectNode authData = pb.collection("users").authWithOAuth2Code(
    "google",
    "AUTHORIZATION_CODE",
    "CODE_VERIFIER",
    "REDIRECT_URL",
    null, null, null, null, null, null, null, null
);
```

## Multi-Factor Authentication (MFA)

Requires 2 different auth methods.

```java
String mfaId = null;

try {
    // First auth method (password)
    pb.collection("users").authWithPassword("test@example.com", "pass123", null, null, null, null, null, null, null, null);
} catch (ClientResponseError err) {
    if (err.response != null && err.response.containsKey("mfaId")) {
        mfaId = err.response.get("mfaId").toString();
        
        // Second auth method (OTP)
        ObjectNode otpResult = pb.collection("users").requestOTP("test@example.com", null, null, null);
        Map<String, Object> mfaOptions = new HashMap<>();
        mfaOptions.put("mfaId", mfaId);
        pb.collection("users").authWithOTP(
            otpResult.path("otpId").asText(),
            "123456",
            mfaOptions,
            null, null, null, null, null, null
        );
    }
}
```

## User Impersonation

Superusers can impersonate other users.

**Backend Endpoint:** `POST /api/collections/{collection}/impersonate/{id}`

```java
// Must be authenticated as superuser
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Impersonate a user
BosBase impersonateClient = pb.collection("users").impersonate("USER_ID", 3600, null, null, null);
// Returns a new client instance with impersonated user's token

// Use the impersonated client
List<ObjectNode> posts = impersonateClient.collection("posts").getFullList(200, null, null, null, null, null, null);

// Access the token
System.out.println(impersonateClient.authStore.getToken());
System.out.println(impersonateClient.authStore.getModel());
```

## Auth Token Verification

Verify token by calling `authRefresh()`.

**Backend Endpoint:** `POST /api/collections/{collection}/auth-refresh`

```java
try {
    ObjectNode authData = pb.collection("users").authRefresh(null, null, null, null);
    System.out.println("Token is valid");
} catch (ClientResponseError err) {
    System.err.println("Token verification failed: " + err);
    pb.authStore.clear();
}
```

## List Available Auth Methods

**Backend Endpoint:** `GET /api/collections/{collection}/auth-methods`

```java
ObjectNode authMethods = pb.collection("users").listAuthMethods(null, null, null, null);
System.out.println(authMethods.path("password").path("enabled").asBoolean());
ArrayNode providers = (ArrayNode) authMethods.path("oauth2").path("providers");
System.out.println(authMethods.path("mfa").path("enabled").asBoolean());
```

## Complete Examples

See the full documentation for detailed examples of:
- Full authentication flow
- OAuth2 integration
- Token management
- Admin impersonation
- Error handling

## Related Documentation

- [Collections](./COLLECTIONS.md)
- [API Rules](./API_RULES_AND_FILTERS.md)

## Detailed Examples

### Example 1: Complete Authentication Flow with Error Handling

```java
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.ClientResponseError;

BosBase pb = new BosBase("http://localhost:8090");

public ObjectNode authenticateUser(String email, String password) {
    try {
        // Try password authentication
        ObjectNode authData = pb.collection("users").authWithPassword(email, password, null, null, null, null, null, null, null, null);
        
        System.out.println("Successfully authenticated: " + authData.path("record").path("email").asText());
        return authData;
        
    } catch (ClientResponseError err) {
        // Check if MFA is required
        if (err.status == 401 && err.response != null && err.response.containsKey("mfaId")) {
            System.out.println("MFA required, proceeding with second factor...");
            return handleMFA(email, err.response.get("mfaId").toString());
        }
        
        // Handle other errors
        if (err.status == 400) {
            throw new RuntimeException("Invalid credentials");
        } else if (err.status == 403) {
            throw new RuntimeException("Password authentication is not enabled for this collection");
        } else {
            throw err;
        }
    }
}

public ObjectNode handleMFA(String email, String mfaId) {
    // Request OTP for second factor
    ObjectNode otpResult = pb.collection("users").requestOTP(email, null, null, null);
    
    // In a real app, show a modal/form for the user to enter OTP
    // For this example, we'll simulate getting the OTP
    String userEnteredOTP = getUserOTPInput(); // Your UI function
    
    try {
        // Authenticate with OTP and MFA ID
        Map<String, Object> mfaOptions = new HashMap<>();
        mfaOptions.put("mfaId", mfaId);
        ObjectNode authData = pb.collection("users").authWithOTP(
            otpResult.path("otpId").asText(),
            userEnteredOTP,
            mfaOptions,
            null, null, null, null, null, null
        );
        
        System.out.println("MFA authentication successful");
        return authData;
    } catch (ClientResponseError err) {
        if (err.status == 429) {
            throw new RuntimeException("Too many OTP attempts, please request a new OTP");
        }
        throw new RuntimeException("Invalid OTP code");
    }
}
```

### Example 2: Token Management and Refresh

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://localhost:8090");

// Check if user is already authenticated
public boolean checkAuth() {
    if (pb.authStore.isValid()) {
        System.out.println("User is authenticated: " + pb.authStore.getModel().path("email").asText());
        
        // Verify token is still valid and refresh if needed
        try {
            pb.collection("users").authRefresh(null, null, null, null);
            System.out.println("Token refreshed successfully");
            return true;
        } catch (ClientResponseError err) {
            System.out.println("Token expired or invalid, clearing auth");
            pb.authStore.clear();
            return false;
        }
    }
    return false;
}
```

## Best Practices

1. **Secure Token Storage**: Never expose tokens in client-side code or logs
2. **Token Refresh**: Implement automatic token refresh before expiration
3. **Error Handling**: Always handle MFA requirements and token expiration
4. **OAuth2 Security**: Always validate the `state` parameter in OAuth2 callbacks
5. **API Keys**: Use impersonation tokens for server-to-server communication only
6. **Superuser Tokens**: Never expose superuser impersonation tokens in client code
7. **OTP Security**: Use OTP with MFA for security-critical applications
8. **Rate Limiting**: Be aware of rate limits on authentication endpoints

## Troubleshooting

### Token Expired
If you get 401 errors, check if the token has expired:
```java
try {
    pb.collection("users").authRefresh(null, null, null, null);
} catch (ClientResponseError err) {
    // Token expired, require re-authentication
    pb.authStore.clear();
    // Redirect to login
}
```

### MFA Required
If authentication returns 401 with mfaId:
```java
if (err.status == 401 && err.response != null && err.response.containsKey("mfaId")) {
    // Proceed with second authentication factor
}
```
