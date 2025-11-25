# Backups API - Java SDK Documentation

## Overview

The Backups API provides endpoints for managing application data backups. You can create backups, upload existing backup files, download backups, delete backups, and restore the application from a backup.

**Key Features:**
- List all available backup files
- Create new backups with custom names or auto-generated names
- Upload existing backup ZIP files
- Download backup files (requires file token)
- Delete backup files
- Restore the application from a backup (restarts the app)

**Backend Endpoints:**
- `GET /api/backups` - List backups
- `POST /api/backups` - Create backup
- `POST /api/backups/upload` - Upload backup
- `GET /api/backups/{key}` - Download backup
- `DELETE /api/backups/{key}` - Delete backup
- `POST /api/backups/{key}/restore` - Restore backup

**Note**: All Backups API operations require superuser authentication (except download which requires a superuser file token).

## Authentication

All Backups API operations require superuser authentication:

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Authenticate as superuser
pb.collection("_superusers").authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
```

**Downloading backups** requires a superuser file token (obtained via `pb.files.getToken()`), but does not require the Authorization header.

## Backup File Structure

Each backup file contains:
- `key`: The filename/key of the backup file (string)
- `size`: File size in bytes (number)
- `modified`: ISO 8601 timestamp of when the backup was last modified (string)

```java
// Backup file structure
{
  "key": "pb_backup_20230519162514.zip",
  "size": 251316185,
  "modified": "2023-05-19T16:25:57.542Z"
}
```

## List Backups

Returns a list of all available backup files with their metadata.

### Basic Usage

```java
// Get all backups
List<ObjectNode> backups = pb.backups.getFullList(null);

for (ObjectNode backup : backups) {
    System.out.println(backup.path("key").asText());
    System.out.println(backup.path("size").asLong());
    System.out.println(backup.path("modified").asText());
}
```

### Working with Backup Lists

```java
// Sort backups by modification date (newest first)
List<ObjectNode> backups = pb.backups.getFullList(null);
backups.sort((a, b) -> {
    String aModified = a.path("modified").asText();
    String bModified = b.path("modified").asText();
    return bModified.compareTo(aModified);
});

// Find the most recent backup
ObjectNode mostRecent = backups.get(0);

// Filter backups by size (larger than 100MB)
List<ObjectNode> largeBackups = backups.stream()
    .filter(backup -> backup.path("size").asLong() > 100 * 1024 * 1024)
    .collect(java.util.stream.Collectors.toList());

// Get total storage used by backups
long totalSize = backups.stream()
    .mapToLong(b -> b.path("size").asLong())
    .sum();
System.out.println("Total backup storage: " + (totalSize / 1024 / 1024) + " MB");
```

## Create Backup

Creates a new backup of the application data. The backup process is asynchronous and may take some time depending on the size of your data.

### Basic Usage

```java
// Create backup with custom name
pb.backups.create("my_backup_2024.zip", null);

// Create backup with auto-generated name (pass empty string or let backend generate)
pb.backups.create("", null);
```

### Backup Name Format

Backup names must follow the format: `[a-z0-9_-].zip`
- Only lowercase letters, numbers, underscores, and hyphens
- Must end with `.zip`
- Maximum length: 150 characters
- Must be unique (no existing backup with the same name)

### Examples

```java
// Create a named backup
public void createNamedBackup(String name) {
    try {
        pb.backups.create(name, null);
        System.out.println("Backup \"" + name + "\" creation initiated");
    } catch (ClientResponseError error) {
        if (error.status == 400) {
            System.err.println("Invalid backup name or backup already exists");
        } else {
            System.err.println("Failed to create backup: " + error);
        }
    }
}

// Create backup with timestamp
public void createTimestampedBackup() {
    String timestamp = java.time.Instant.now().toString()
        .replace(":", "-")
        .substring(0, 19);
    String name = "backup_" + timestamp + ".zip";
    pb.backups.create(name, null);
}
```

### Important Notes

- **Asynchronous Process**: Backup creation happens in the background. The API returns immediately (204 No Content).
- **Concurrent Operations**: Only one backup or restore operation can run at a time. If another operation is in progress, you'll receive a 400 error.
- **Storage**: Backups are stored in the configured backup filesystem (local or S3).
- **S3 Consistency**: For S3 storage, the backup file may not be immediately available after creation due to eventual consistency.

## Upload Backup

Uploads an existing backup ZIP file to the server. This is useful for restoring backups created elsewhere or for importing backups.

### Basic Usage

```java
// Upload from a File object or byte array
byte[] backupBytes = Files.readAllBytes(Paths.get("backup.zip"));
Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
fileList.add(new FileAttachment(backupBytes, "backup.zip", "application/zip"));
files.put("file", fileList);

pb.backups.upload(files, null);
```

### File Requirements

- **MIME Type**: Must be `application/zip`
- **Format**: Must be a valid ZIP archive
- **Name**: Must be unique (no existing backup with the same name)
- **Validation**: The file will be validated before upload

## Download Backup

Downloads a backup file. Requires a superuser file token for authentication.

### Basic Usage

```java
// Get file token
String token = pb.files.getToken(null);

// Build download URL
String url = pb.backups.getDownloadURL(token, "pb_backup_20230519162514.zip");

// Download the file using HTTP client
// Use the URL to download the backup file
```

### Download URL Structure

The download URL format is:
```
/api/backups/{key}?token={fileToken}
```

### Examples

```java
// Download backup function
public void downloadBackup(String backupKey) {
    try {
        // Get file token (valid for short period)
        String token = pb.files.getToken(null);
        
        // Build download URL
        String url = pb.backups.getDownloadURL(token, backupKey);
        
        // Use HTTP client to download
        System.out.println("Download URL: " + url);
    } catch (ClientResponseError error) {
        System.err.println("Failed to download backup: " + error);
    }
}
```

## Delete Backup

Deletes a backup file from the server.

### Basic Usage

```java
pb.backups.delete("pb_backup_20230519162514.zip", null);
```

### Important Notes

- **Active Backups**: Cannot delete a backup that is currently being created or restored
- **No Undo**: Deletion is permanent
- **File System**: The file will be removed from the backup filesystem

### Examples

```java
// Delete backup with confirmation
public void deleteBackupWithConfirmation(String backupKey) {
    // In a real app, show confirmation dialog
    try {
        pb.backups.delete(backupKey, null);
        System.out.println("Backup deleted successfully");
    } catch (ClientResponseError error) {
        if (error.status == 400) {
            System.err.println("Backup is currently in use and cannot be deleted");
        } else if (error.status == 404) {
            System.err.println("Backup not found");
        } else {
            System.err.println("Failed to delete backup: " + error);
        }
    }
}

// Delete old backups (older than 30 days)
public void deleteOldBackups() {
    List<ObjectNode> backups = pb.backups.getFullList(null);
    java.time.Instant thirtyDaysAgo = java.time.Instant.now().minusSeconds(30 * 24 * 60 * 60);
    
    for (ObjectNode backup : backups) {
        String modified = backup.path("modified").asText();
        java.time.Instant modifiedTime = java.time.Instant.parse(modified);
        
        if (modifiedTime.isBefore(thirtyDaysAgo)) {
            try {
                pb.backups.delete(backup.path("key").asText(), null);
                System.out.println("Deleted old backup: " + backup.path("key").asText());
            } catch (ClientResponseError error) {
                System.err.println("Failed to delete " + backup.path("key").asText() + ": " + error);
            }
        }
    }
}
```

## Restore Backup

Restores the application from a backup file. **This operation will restart the application**.

### Basic Usage

```java
pb.backups.restore("pb_backup_20230519162514.zip", null);
```

### Important Warnings

⚠️ **CRITICAL**: Restoring a backup will:
1. Replace all current application data with data from the backup
2. **Restart the application process**
3. Any unsaved changes will be lost
4. The application will be unavailable during the restore process

### Prerequisites

- **Disk Space**: Recommended to have at least **2x the backup size** in free disk space
- **UNIX Systems**: Restore is primarily supported on UNIX-based systems (Linux, macOS)
- **No Concurrent Operations**: Cannot restore if another backup or restore is in progress
- **Backup Existence**: The backup file must exist on the server

### Examples

```java
// Restore backup with confirmation
public void restoreBackupWithConfirmation(String backupKey) {
    // In a real app, show confirmation dialog
    try {
        pb.backups.restore(backupKey, null);
        System.out.println("Restore initiated. Application will restart...");
    } catch (ClientResponseError error) {
        if (error.status == 400) {
            System.err.println("Another backup or restore operation is in progress");
        } else {
            System.err.println("Failed to restore backup: " + error);
        }
    }
}
```

## Complete Examples

### Example 1: Backup Manager Class

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

public class BackupManager {
    private BosBase pb;
    
    public BackupManager(BosBase pb) {
        this.pb = pb;
    }
    
    public List<ObjectNode> list() {
        List<ObjectNode> backups = pb.backups.getFullList(null);
        backups.sort((a, b) -> {
            String aModified = a.path("modified").asText();
            String bModified = b.path("modified").asText();
            return bModified.compareTo(aModified);
        });
        return backups;
    }
    
    public String create(String name) {
        if (name == null || name.isEmpty()) {
            String timestamp = java.time.Instant.now().toString()
                .replace(":", "-")
                .substring(0, 19);
            name = "backup_" + timestamp + ".zip";
        }
        pb.backups.create(name, null);
        return name;
    }
    
    public String download(String key) {
        String token = pb.files.getToken(null);
        return pb.backups.getDownloadURL(token, key);
    }
    
    public void delete(String key) {
        pb.backups.delete(key, null);
    }
    
    public void restore(String key) {
        pb.backups.restore(key, null);
    }
    
    public int cleanup(int daysOld) {
        List<ObjectNode> backups = list();
        java.time.Instant cutoff = java.time.Instant.now().minusSeconds(daysOld * 24 * 60 * 60);
        
        int deleted = 0;
        for (ObjectNode backup : backups) {
            String modified = backup.path("modified").asText();
            java.time.Instant modifiedTime = java.time.Instant.parse(modified);
            
            if (modifiedTime.isBefore(cutoff)) {
                try {
                    delete(backup.path("key").asText());
                    System.out.println("Deleted: " + backup.path("key").asText());
                    deleted++;
                } catch (ClientResponseError error) {
                    System.err.println("Failed to delete " + backup.path("key").asText() + ": " + error);
                }
            }
        }
        
        return deleted;
    }
}
```

## Error Handling

```java
// Handle common backup errors
public void handleBackupError(String operation, String... args) {
    try {
        switch (operation) {
            case "create":
                pb.backups.create(args[0], null);
                break;
            case "delete":
                pb.backups.delete(args[0], null);
                break;
            case "restore":
                pb.backups.restore(args[0], null);
                break;
        }
    } catch (ClientResponseError error) {
        switch (error.status) {
            case 400:
                if (error.message != null && error.message.contains("another backup/restore")) {
                    System.err.println("Another backup or restore operation is in progress");
                } else if (error.message != null && error.message.contains("already exists")) {
                    System.err.println("Backup with this name already exists");
                } else {
                    System.err.println("Invalid request: " + error.message);
                }
                break;
            
            case 401:
                System.err.println("Not authenticated");
                break;
            
            case 403:
                System.err.println("Not a superuser");
                break;
            
            case 404:
                System.err.println("Backup not found");
                break;
            
            default:
                System.err.println("Unexpected error: " + error);
        }
        throw error;
    }
}
```

## Best Practices

1. **Regular Backups**: Create backups regularly (daily, weekly, or based on your needs)
2. **Naming Convention**: Use clear, consistent naming (e.g., `backup_YYYY-MM-DD.zip`)
3. **Backup Rotation**: Implement cleanup to remove old backups and prevent storage issues
4. **Test Restores**: Periodically test restoring backups to ensure they work
5. **Off-site Storage**: Download and store backups in a separate location
6. **Pre-Restore Backup**: Always create a backup before restoring (if possible)
7. **Monitor Storage**: Monitor backup storage usage to prevent disk space issues
8. **Documentation**: Document your backup and restore procedures
9. **Automation**: Use cron jobs or schedulers for automated backups
10. **Verification**: Verify backup integrity after creation/download

## Limitations

- **Superuser Only**: All operations require superuser authentication
- **Concurrent Operations**: Only one backup or restore can run at a time
- **Restore Restart**: Restoring a backup restarts the application
- **UNIX Systems**: Restore primarily works on UNIX-based systems
- **Disk Space**: Restore requires significant free disk space (2x backup size recommended)
- **S3 Consistency**: S3 backups may not be immediately available after creation
- **Active Backups**: Cannot delete backups that are currently being created or restored

## Related Documentation

- [File API](./FILE_API.md) - File handling and tokens
- [Crons API](./CRONS_API.md) - Automated backup scheduling
- [Collection API](./COLLECTION_API.md) - Collection management

