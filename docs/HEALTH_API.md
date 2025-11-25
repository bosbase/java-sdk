# Health API - Java SDK Documentation

## Overview

The Health API provides a simple endpoint to check the health status of the server. It returns basic health information and, when authenticated as a superuser, provides additional diagnostic information about the server state.

**Key Features:**
- No authentication required for basic health check
- Superuser authentication provides additional diagnostic data
- Lightweight endpoint for monitoring and health checks
- Supports both GET and HEAD methods

**Backend Endpoints:**
- `GET /api/health` - Check health status
- `HEAD /api/health` - Check health status (HEAD method)

**Note**: The health endpoint is publicly accessible, but superuser authentication provides additional information.

## Authentication

Basic health checks do not require authentication:

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Basic health check (no auth required)
ObjectNode health = pb.health.check();
```

For additional diagnostic information, authenticate as a superuser:

```java
// Authenticate as superuser for extended health data
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
ObjectNode health = pb.health.check();
```

## Health Check Response Structure

### Basic Response (Guest/Regular User)

```java
{
  "code": 200,
  "message": "API is healthy.",
  "data": {}
}
```

### Superuser Response

```java
{
  "code": 200,
  "message": "API is healthy.",
  "data": {
    "canBackup": boolean,           // Whether backup operations are allowed
    "realIP": string,               // Real IP address of the client
    "requireS3": boolean,           // Whether S3 storage is required
    "possibleProxyHeader": string   // Detected proxy header (if behind reverse proxy)
  }
}
```

## Check Health Status

Returns the health status of the API server.

### Basic Usage

```java
// Simple health check
ObjectNode health = pb.health.check();

System.out.println(health.get("message").asText()); // "API is healthy."
System.out.println(health.get("code").asInt());    // 200
```

### With Superuser Authentication

```java
// Authenticate as superuser first
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Get extended health information
ObjectNode health = pb.health.check();

ObjectNode data = (ObjectNode) health.get("data");
System.out.println(data.get("canBackup").asBoolean());           // true/false
System.out.println(data.get("realIP").asText());                  // "192.168.1.100"
System.out.println(data.get("requireS3").asBoolean());           // false
System.out.println(data.get("possibleProxyHeader").asText());     // "" or header name
```

## Response Fields

### Common Fields (All Users)

| Field | Type | Description |
|-------|------|-------------|
| `code` | number | HTTP status code (always 200 for healthy server) |
| `message` | string | Health status message ("API is healthy.") |
| `data` | object | Health data (empty for non-superusers, populated for superusers) |

### Superuser-Only Fields (in `data`)

| Field | Type | Description |
|-------|------|-------------|
| `canBackup` | boolean | `true` if backup/restore operations can be performed, `false` if a backup/restore is currently in progress |
| `realIP` | string | The real IP address of the client (useful when behind proxies) |
| `requireS3` | boolean | `true` if S3 storage is required (local fallback disabled), `false` otherwise |
| `possibleProxyHeader` | string | Detected proxy header name (e.g., "X-Forwarded-For", "CF-Connecting-IP") if the server appears to be behind a reverse proxy, empty string otherwise |

## Use Cases

### 1. Basic Health Monitoring

```java
public boolean checkServerHealth() {
    try {
        ObjectNode health = pb.health.check();
        
        if (health.get("code").asInt() == 200 && 
            "API is healthy.".equals(health.get("message").asText())) {
            System.out.println("✓ Server is healthy");
            return true;
        } else {
            System.out.println("✗ Server health check failed");
            return false;
        }
    } catch (Exception e) {
        System.err.println("✗ Health check error: " + e.getMessage());
        return false;
    }
}

// Use in monitoring
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    boolean isHealthy = checkServerHealth();
    if (!isHealthy) {
        System.err.println("Server health check failed!");
    }
}, 0, 60, TimeUnit.SECONDS); // Check every minute
```

### 2. Backup Readiness Check

```java
public boolean canPerformBackup() {
    try {
        // Authenticate as superuser
        pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
        
        ObjectNode health = pb.health.check();
        ObjectNode data = (ObjectNode) health.get("data");
        
        if (data.has("canBackup") && !data.get("canBackup").asBoolean()) {
            System.out.println("⚠️ Backup operation is currently in progress");
            return false;
        }
        
        System.out.println("✓ Backup operations are allowed");
        return true;
    } catch (Exception e) {
        System.err.println("Failed to check backup readiness: " + e.getMessage());
        return false;
    }
}

// Use before creating backups
if (canPerformBackup()) {
    pb.backups.create("backup.zip", null);
}
```

### 3. Monitoring Dashboard

```java
public class HealthMonitor {
    private BosBase pb;
    private boolean isSuperuser = false;

    public HealthMonitor(BosBase pb) {
        this.pb = pb;
    }

    public boolean authenticateAsSuperuser(String email, String password) {
        try {
            pb.admins().authWithPassword(email, password, null, null, null, null, null, null, null, null);
            this.isSuperuser = true;
            return true;
        } catch (Exception e) {
            System.err.println("Superuser authentication failed: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getHealthStatus() {
        try {
            ObjectNode health = pb.health.check();
            
            Map<String, Object> status = new HashMap<>();
            status.put("healthy", health.get("code").asInt() == 200);
            status.put("message", health.get("message").asText());
            status.put("timestamp", System.currentTimeMillis());
            
            if (isSuperuser && health.has("data")) {
                ObjectNode data = (ObjectNode) health.get("data");
                Map<String, Object> diagnostics = new HashMap<>();
                diagnostics.put("canBackup", data.has("canBackup") ? data.get("canBackup").asBoolean() : null);
                diagnostics.put("realIP", data.has("realIP") ? data.get("realIP").asText() : null);
                diagnostics.put("requireS3", data.has("requireS3") ? data.get("requireS3").asBoolean() : null);
                diagnostics.put("behindProxy", data.has("possibleProxyHeader") && 
                    !data.get("possibleProxyHeader").asText().isEmpty());
                diagnostics.put("proxyHeader", data.has("possibleProxyHeader") ? 
                    data.get("possibleProxyHeader").asText() : null);
                status.put("diagnostics", diagnostics);
            }
            
            return status;
        } catch (Exception e) {
            Map<String, Object> status = new HashMap<>();
            status.put("healthy", false);
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
            return status;
        }
    }

    public void startMonitoring(long intervalMs) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            Map<String, Object> status = getHealthStatus();
            System.out.println("Health Status: " + status);
            
            if (!(Boolean) status.get("healthy")) {
                onHealthIssue(status);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void onHealthIssue(Map<String, Object> status) {
        System.err.println("Health issue detected: " + status);
        // Implement alerting logic here
    }
}

// Usage
HealthMonitor monitor = new HealthMonitor(pb);
monitor.authenticateAsSuperuser("admin@example.com", "password");
monitor.startMonitoring(30000); // Check every 30 seconds
```

### 4. Load Balancer Health Check

```java
// Simple health check for load balancers
public boolean simpleHealthCheck() {
    try {
        ObjectNode health = pb.health.check();
        return health.get("code").asInt() == 200;
    } catch (Exception e) {
        return false;
    }
}

// Use in web application endpoint
@GetMapping("/health")
public ResponseEntity<Map<String, String>> health() {
    boolean isHealthy = simpleHealthCheck();
    Map<String, String> response = new HashMap<>();
    if (isHealthy) {
        response.put("status", "healthy");
        return ResponseEntity.ok(response);
    } else {
        response.put("status", "unhealthy");
        return ResponseEntity.status(503).body(response);
    }
}
```

### 5. Proxy Detection

```java
public Map<String, Object> checkProxySetup() throws Exception {
    pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
    
    ObjectNode health = pb.health.check();
    ObjectNode data = (ObjectNode) health.get("data");
    String proxyHeader = data.has("possibleProxyHeader") ? 
        data.get("possibleProxyHeader").asText() : "";
    
    if (!proxyHeader.isEmpty()) {
        System.out.println("⚠️ Server appears to be behind a reverse proxy");
        System.out.println("   Detected proxy header: " + proxyHeader);
        System.out.println("   Real IP: " + data.get("realIP").asText());
        System.out.println("   Ensure TrustedProxy settings are configured correctly in admin panel");
    } else {
        System.out.println("✓ No reverse proxy detected (or properly configured)");
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("behindProxy", !proxyHeader.isEmpty());
    result.put("proxyHeader", proxyHeader.isEmpty() ? null : proxyHeader);
    result.put("realIP", data.has("realIP") ? data.get("realIP").asText() : null);
    
    return result;
}
```

### 6. Pre-Flight Checks

```java
public Map<String, Object> preFlightCheck() {
    Map<String, Object> checks = new HashMap<>();
    checks.put("serverHealthy", false);
    checks.put("canBackup", false);
    checks.put("storageConfigured", false);
    List<String> issues = new ArrayList<>();
    checks.put("issues", issues);
    
    try {
        // Basic health check
        ObjectNode health = pb.health.check();
        checks.put("serverHealthy", health.get("code").asInt() == 200);
        
        if (!(Boolean) checks.get("serverHealthy")) {
            issues.add("Server health check failed");
            return checks;
        }
        
        // Authenticate as superuser for extended checks
        try {
            pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
            
            ObjectNode detailedHealth = pb.health.check();
            ObjectNode data = (ObjectNode) detailedHealth.get("data");
            
            checks.put("canBackup", data.has("canBackup") && data.get("canBackup").asBoolean());
            checks.put("storageConfigured", !data.has("requireS3") || !data.get("requireS3").asBoolean());
            
            if (!(Boolean) checks.get("canBackup")) {
                issues.add("Backup operations are currently unavailable");
            }
            
            if (data.has("requireS3") && data.get("requireS3").asBoolean()) {
                issues.add("S3 storage is required but may not be configured");
            }
        } catch (Exception authError) {
            issues.add("Superuser authentication failed - limited diagnostics available");
        }
    } catch (Exception e) {
        issues.add("Health check error: " + e.getMessage());
    }
    
    return checks;
}

// Use before critical operations
Map<String, Object> checks = preFlightCheck();
@SuppressWarnings("unchecked")
List<String> issues = (List<String>) checks.get("issues");
if (!issues.isEmpty()) {
    System.err.println("Pre-flight check issues: " + issues);
    // Handle issues before proceeding
}
```

### 7. Automated Backup Scheduler

```java
public class BackupScheduler {
    private BosBase pb;

    public BackupScheduler(BosBase pb) {
        this.pb = pb;
    }

    public boolean waitForBackupAvailability(long maxWaitMs) {
        long startTime = System.currentTimeMillis();
        long checkInterval = 5000; // Check every 5 seconds
        
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                ObjectNode health = pb.health.check();
                ObjectNode data = (ObjectNode) health.get("data");
                
                if (data.has("canBackup") && data.get("canBackup").asBoolean()) {
                    return true;
                }
                
                System.out.println("Backup in progress, waiting...");
                Thread.sleep(checkInterval);
            } catch (Exception e) {
                System.err.println("Health check failed: " + e.getMessage());
                return false;
            }
        }
        
        System.err.println("Timeout waiting for backup availability");
        return false;
    }

    public void scheduleBackup(String backupName) throws Exception {
        // Wait for backup operations to be available
        boolean isAvailable = waitForBackupAvailability(300000); // 5 minutes max
        
        if (!isAvailable) {
            throw new RuntimeException("Backup operations are not available");
        }
        
        // Create the backup
        pb.backups.create(backupName, null);
        System.out.println("Backup \"" + backupName + "\" created");
    }
}

// Usage
BackupScheduler scheduler = new BackupScheduler(pb);
scheduler.scheduleBackup("scheduled_backup.zip");
```

## Error Handling

```java
public Map<String, Object> safeHealthCheck() {
    try {
        ObjectNode health = pb.health.check();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", health);
        return result;
    } catch (Exception e) {
        // Network errors, server down, etc.
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", e.getMessage());
        result.put("code", 0);
        return result;
    }
}

// Handle different error scenarios
Map<String, Object> result = safeHealthCheck();
if (!(Boolean) result.get("success")) {
    int code = (Integer) result.get("code");
    if (code == 0) {
        System.err.println("Network error or server unreachable");
    } else {
        System.err.println("Server returned error: " + code);
    }
}
```

## Best Practices

1. **Monitoring**: Use health checks for regular monitoring (e.g., every 30-60 seconds)
2. **Load Balancers**: Configure load balancers to use the health endpoint for health checks
3. **Pre-flight Checks**: Check `canBackup` before initiating backup operations
4. **Error Handling**: Always handle errors gracefully as the server may be down
5. **Rate Limiting**: Don't poll the health endpoint too frequently (avoid spamming)
6. **Caching**: Consider caching health check results for a few seconds to reduce load
7. **Logging**: Log health check results for troubleshooting and monitoring
8. **Alerting**: Set up alerts for consecutive health check failures
9. **Superuser Auth**: Only authenticate as superuser when you need diagnostic information
10. **Proxy Configuration**: Use `possibleProxyHeader` to detect and configure reverse proxy settings

## Response Codes

| Code | Meaning |
|------|---------|
| 200 | Server is healthy |
| Network Error | Server is unreachable or down |

## Limitations

- **No Detailed Metrics**: The health endpoint does not provide detailed performance metrics
- **Basic Status Only**: Returns basic status, not detailed system information
- **Superuser Required**: Extended diagnostics require superuser authentication
- **No Historical Data**: Only returns current status, no historical health data

## Related Documentation

- [Backups API](./BACKUPS_API.md) - Using `canBackup` to check backup readiness
- [Authentication](./AUTHENTICATION.md) - Superuser authentication
- [Settings API](./SETTINGS_API.md) - Configuring trusted proxy settings

