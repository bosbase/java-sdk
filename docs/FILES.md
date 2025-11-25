# Files Upload and Handling - Java SDK Documentation

## Overview

BosBase allows you to upload and manage files through file fields in your collections. Files are stored with sanitized names and a random suffix for security (e.g., `test_52iwbgds7l.png`).

**Key Features:**
- Upload multiple files per field
- Maximum file size: ~8GB (2^53-1 bytes)
- Automatic filename sanitization and random suffix
- Image thumbnails support
- Protected files with token-based access
- File modifiers for append/prepend/delete operations

**Backend Endpoints:**
- `POST /api/files/token` - Get file access token for protected files
- `GET /api/files/{collection}/{recordId}/{filename}` - Download file

## File Field Configuration

Before uploading files, you must add a file field to your collection:

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Get collection
ObjectNode collection = pb.collections.getOne("example", null, null, null, null);

// Add file field
ArrayNode fields = (ArrayNode) collection.get("fields");
ObjectNode fileField = JsonUtils.createObjectNode();
fileField.put("name", "documents");
fileField.put("type", "file");
fileField.put("maxSelect", 5);  // Maximum number of files (1 for single file)
fileField.put("maxSize", 5242880);  // 5MB in bytes (optional, default: 5MB)

ArrayNode mimeTypes = JsonUtils.createArrayNode();
mimeTypes.add("image/jpeg");
mimeTypes.add("image/png");
mimeTypes.add("application/pdf");
fileField.set("mimeTypes", mimeTypes);

ArrayNode thumbs = JsonUtils.createArrayNode();
thumbs.add("100x100");
thumbs.add("300x300");
fileField.set("thumbs", thumbs);  // Thumbnail sizes for images
fileField.put("protected", false);  // Require token for access

fields.add(fileField);

Map<String, Object> updateData = new HashMap<>();
updateData.put("fields", fields);
pb.collections.update("example", updateData, null, null);
```

## Uploading Files

### Basic Upload with Create

When creating a new record, you can upload files directly:

```java
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.FileAttachment;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");

// Method 1: Using FileAttachment objects
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "Hello world!");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "file1.txt", "text/plain"));
// fileList.add(new FileAttachment(fileBytes2, "file2.txt", "text/plain"));
files.put("documents", fileList);

ObjectNode createdRecord = pb.collection("example").create(recordData, files, null, null);
```

### Upload with Update

```java
// Update record and upload new files
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated title");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(newFileBytes, "file3.txt", "text/plain"));
files.put("documents", fileList);

ObjectNode updatedRecord = pb.collection("example").update("RECORD_ID", updateData, files, null, null);
```

### Append Files (Using + Modifier)

For multiple file fields, use the `+` modifier to append files:

```java
// Append files to existing ones
Map<String, Object> updateData = new HashMap<>();
Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "file4.txt", "text/plain"));
files.put("documents+", fileList);

pb.collection("example").update("RECORD_ID", updateData, files, null, null);

// Or prepend files (files will appear first)
files.clear();
fileList.clear();
// fileList.add(new FileAttachment(fileBytes, "file0.txt", "text/plain"));
files.put("+documents", fileList);

pb.collection("example").update("RECORD_ID", updateData, files, null, null);
```

### Upload Multiple Files with Modifiers

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();

// Append multiple files
for (FileAttachment file : selectedFiles) {
    fileList.add(file);
}
files.put("documents+", fileList);

pb.collection("example").update("RECORD_ID", updateData, files, null, null);
```

## Deleting Files

### Delete All Files

```java
// Delete all files in a field (set to empty array)
Map<String, Object> updateData = new HashMap<>();
updateData.put("documents", new ArrayList<>());

pb.collection("example").update("RECORD_ID", updateData, null, null, null);
```

### Delete Specific Files (Using - Modifier)

```java
// Delete individual files by filename
Map<String, Object> updateData = new HashMap<>();
updateData.put("documents-", List.of("file1.pdf", "file2.txt"));

pb.collection("example").update("RECORD_ID", updateData, null, null, null);
```

## File URLs

### Get File URL

Each uploaded file can be accessed via its URL:

```
http://localhost:8090/api/files/COLLECTION_ID_OR_NAME/RECORD_ID/FILENAME
```

**Using SDK:**

```java
ObjectNode record = pb.collection("example").getOne("RECORD_ID", null, null, null, null);

// Single file field (returns string)
String filename = record.get("documents").asText();
String url = pb.files.getURL(record, filename);

// Multiple file field (returns array)
if (record.get("documents").isArray()) {
    String firstFile = record.get("documents").get(0).asText();
    String url = pb.files.getURL(record, firstFile);
}
```

### Image Thumbnails

If your file field has thumbnail sizes configured, you can request thumbnails:

```java
ObjectNode record = pb.collection("example").getOne("RECORD_ID", null, null, null, null);
String filename = record.get("avatar").asText();  // Image file

// Get thumbnail with specific size
Map<String, String> options = new HashMap<>();
options.put("thumb", "100x300");
String thumbUrl = pb.files.getURL(record, filename, options);
```

**Thumbnail Formats:**

- `WxH` (e.g., `100x300`) - Crop to WxH viewbox from center
- `WxHt` (e.g., `100x300t`) - Crop to WxH viewbox from top
- `WxHb` (e.g., `100x300b`) - Crop to WxH viewbox from bottom
- `WxHf` (e.g., `100x300f`) - Fit inside WxH viewbox (no cropping)
- `0xH` (e.g., `0x300`) - Resize to H height, preserve aspect ratio
- `Wx0` (e.g., `100x0`) - Resize to W width, preserve aspect ratio

**Supported Image Formats:**
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- GIF (`.gif` - first frame only)
- WebP (`.webp` - stored as PNG)

**Example:**

```java
ObjectNode record = pb.collection("products").getOne("PRODUCT_ID", null, null, null, null);
String image = record.get("image").asText();

// Different thumbnail sizes
Map<String, String> options1 = new HashMap<>();
options1.put("thumb", "100x100");
String thumbSmall = pb.files.getURL(record, image, options1);

Map<String, String> options2 = new HashMap<>();
options2.put("thumb", "300x300f");
String thumbMedium = pb.files.getURL(record, image, options2);

Map<String, String> options3 = new HashMap<>();
options3.put("thumb", "800x600");
String thumbLarge = pb.files.getURL(record, image, options3);
```

### Force Download

To force browser download instead of preview:

```java
Map<String, String> options = new HashMap<>();
options.put("download", "1");  // Force download
String url = pb.files.getURL(record, filename, options);
```

## Protected Files

By default, all files are publicly accessible if you know the full URL. For sensitive files, you can mark the field as "Protected" in the collection settings.

### Setting Up Protected Files

```java
ObjectNode collection = pb.collections.getOne("example", null, null, null, null);

ArrayNode fields = (ArrayNode) collection.get("fields");
for (JsonNode field : fields) {
    if ("documents".equals(field.get("name").asText())) {
        ((ObjectNode) field).put("protected", true);
        break;
    }
}

Map<String, Object> updateData = new HashMap<>();
updateData.put("fields", fields);
pb.collections.update("example", updateData, null, null);
```

### Accessing Protected Files

Protected files require authentication and a file token:

```java
// Step 1: Authenticate
pb.collection("users").authWithPassword("user@example.com", "password123", null, null, null, null, null, null, null, null);

// Step 2: Get file token (valid for ~2 minutes)
String fileToken = pb.files.getToken();

// Step 3: Get protected file URL with token
ObjectNode record = pb.collection("example").getOne("RECORD_ID", null, null, null, null);
Map<String, String> options = new HashMap<>();
options.put("token", fileToken);
String url = pb.files.getURL(record, record.get("privateDocument").asText(), options);

// Use the URL (e.g., in a web view or download)
System.out.println("Protected file URL: " + url);
```

**Important:**
- File tokens are short-lived (~2 minutes)
- Only authenticated users satisfying the collection's `viewRule` can access protected files
- Tokens must be regenerated when they expire

### Complete Protected File Example

```java
public String loadProtectedImage(String recordId, String filename) {
    try {
        // Check if authenticated
        if (!pb.authStore.isValid()) {
            throw new RuntimeException("Not authenticated");
        }

        // Get fresh token
        String token = pb.files.getToken();

        // Get file URL
        ObjectNode record = pb.collection("example").getOne(recordId, null, null, null, null);
        Map<String, String> options = new HashMap<>();
        options.put("token", token);
        String url = pb.files.getURL(record, filename, options);

        return url;
    } catch (Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("404")) {
            System.err.println("File not found or access denied");
        } else if (e.getMessage() != null && e.getMessage().contains("401")) {
            System.err.println("Authentication required");
            pb.authStore.clear();
        }
        throw new RuntimeException("Failed to load protected file", e);
    }
}
```

## Complete Examples

### Example 1: Image Upload with Thumbnails

```java
import com.bosbase.sdk.BosBase;
import com.bosbase.sdk.FileAttachment;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Create collection with image field and thumbnails
Map<String, Object> collectionData = new HashMap<>();
collectionData.put("name", "products");
collectionData.put("type", "base");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> nameField = new HashMap<>();
nameField.put("name", "name");
nameField.put("type", "text");
nameField.put("required", true);
fields.add(nameField);

Map<String, Object> imageField = new HashMap<>();
imageField.put("name", "image");
imageField.put("type", "file");
imageField.put("maxSelect", 1);
imageField.put("mimeTypes", List.of("image/jpeg", "image/png"));
imageField.put("thumbs", List.of("100x100", "300x300", "800x600f"));  // Thumbnail sizes
fields.add(imageField);

collectionData.put("fields", fields);
ObjectNode collection = pb.collections.create(collectionData, null, null, null);

// Upload product with image
Map<String, Object> productData = new HashMap<>();
productData.put("name", "My Product");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(imageBytes, "product.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode product = pb.collection("products").create(productData, files, null, null);

// Display thumbnail in UI
Map<String, String> options = new HashMap<>();
options.put("thumb", "300x300");
String thumbnailUrl = pb.files.getURL(product, product.get("image").asText(), options);
System.out.println("Thumbnail URL: " + thumbnailUrl);
```

### Example 2: Multiple File Upload

```java
public void uploadMultipleFiles(String recordId, List<FileAttachment> files) {
    Map<String, Object> updateData = new HashMap<>();
    updateData.put("title", "Document Set");

    Map<String, List<FileAttachment>> fileMap = new HashMap<>();
    fileMap.put("documents", files);

    try {
        ObjectNode record = pb.collection("example").update(recordId, updateData, fileMap, null, null);
        System.out.println("Uploaded files: " + record.get("documents"));
    } catch (Exception e) {
        System.err.println("Upload failed: " + e.getMessage());
    }
}
```

### Example 3: File Management

```java
public class FileManager {
    private BosBase pb;
    private String collectionId;
    private String recordId;
    private ObjectNode record;

    public FileManager(BosBase pb, String collectionId, String recordId) {
        this.pb = pb;
        this.collectionId = collectionId;
        this.recordId = recordId;
    }

    public void load() throws Exception {
        this.record = pb.collection(collectionId).getOne(recordId, null, null, null, null);
    }

    public List<String> getFiles() {
        JsonNode documents = record.get("documents");
        List<String> files = new ArrayList<>();
        
        if (documents.isArray()) {
            for (JsonNode file : documents) {
                files.add(file.asText());
            }
        } else if (documents != null && !documents.isNull()) {
            files.add(documents.asText());
        }
        
        return files;
    }

    public String getFileUrl(String filename) {
        return pb.files.getURL(record, filename);
    }

    public void deleteFile(String filename) throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("documents-", List.of(filename));
        pb.collection(collectionId).update(recordId, updateData, null, null, null);
        load();  // Reload
    }

    public void addFiles(List<FileAttachment> files) throws Exception {
        Map<String, List<FileAttachment>> fileMap = new HashMap<>();
        fileMap.put("documents+", files);
        pb.collection(collectionId).update(recordId, new HashMap<>(), fileMap, null, null);
        load();  // Reload
    }
}

// Usage
FileManager manager = new FileManager(pb, "example", "RECORD_ID");
manager.load();
List<String> files = manager.getFiles();
for (String filename : files) {
    System.out.println("File: " + manager.getFileUrl(filename));
}
```

### Example 4: Protected Document Viewer

```java
public String viewProtectedDocument(String recordId, String filename) throws Exception {
    // Authenticate if needed
    if (!pb.authStore.isValid()) {
        pb.collection("users").authWithPassword("user@example.com", "pass", null, null, null, null, null, null, null, null);
    }

    // Get token
    String token;
    try {
        token = pb.files.getToken();
    } catch (Exception e) {
        System.err.println("Failed to get file token: " + e.getMessage());
        return null;
    }

    // Get record and file URL
    ObjectNode record = pb.collection("documents").getOne(recordId, null, null, null, null);
    Map<String, String> options = new HashMap<>();
    options.put("token", token);
    String url = pb.files.getURL(record, filename, options);

    return url;
}
```

### Example 5: Image Gallery with Thumbnails

```java
public void displayImageGallery(String recordId) throws Exception {
    ObjectNode record = pb.collection("gallery").getOne(recordId, null, null, null, null);
    JsonNode images = record.get("images");
    
    List<String> imageList = new ArrayList<>();
    if (images.isArray()) {
        for (JsonNode img : images) {
            imageList.add(img.asText());
        }
    }
    
    for (String filename : imageList) {
        // Thumbnail for grid view
        Map<String, String> thumbOptions = new HashMap<>();
        thumbOptions.put("thumb", "200x200f");  // Fit inside 200x200
        String thumbUrl = pb.files.getURL(record, filename, thumbOptions);
        
        // Full size for lightbox
        Map<String, String> fullOptions = new HashMap<>();
        fullOptions.put("thumb", "1200x800f");  // Larger size
        String fullUrl = pb.files.getURL(record, filename, fullOptions);
        
        System.out.println("Thumbnail: " + thumbUrl);
        System.out.println("Full: " + fullUrl);
    }
}
```

## File Field Modifiers

### Summary

- **No modifier** - Replace all files: `documents: [file1, file2]`
- **`+` suffix** - Append files: `documents+: file3`
- **`+` prefix** - Prepend files: `+documents: file0`
- **`-` suffix** - Delete files: `documents-: ['file1.pdf']`

## Best Practices

1. **File Size Limits**: Always validate file sizes on the client before upload
2. **MIME Types**: Configure allowed MIME types in collection field settings
3. **Thumbnails**: Pre-generate common thumbnail sizes for better performance
4. **Protected Files**: Use protected files for sensitive documents (ID cards, contracts)
5. **Token Refresh**: Refresh file tokens before they expire for protected files
6. **Error Handling**: Handle 404 errors for missing files and 401 for protected file access
7. **Filename Sanitization**: Files are automatically sanitized, but validate on client side too

## Error Handling

```java
try {
    Map<String, Object> recordData = new HashMap<>();
    recordData.put("title", "Test");
    
    Map<String, List<FileAttachment>> files = new HashMap<>();
    List<FileAttachment> fileList = new ArrayList<>();
    // fileList.add(new FileAttachment(fileBytes, "test.txt", "text/plain"));
    files.put("documents", fileList);
    
    ObjectNode record = pb.collection("example").create(recordData, files, null, null);
} catch (Exception e) {
    if (e.getMessage() != null && e.getMessage().contains("413")) {
        System.err.println("File too large");
    } else if (e.getMessage() != null && e.getMessage().contains("400")) {
        System.err.println("Invalid file type or field validation failed");
    } else if (e.getMessage() != null && e.getMessage().contains("403")) {
        System.err.println("Insufficient permissions");
    } else {
        System.err.println("Upload failed: " + e.getMessage());
    }
}
```

## Storage Options

By default, BosBase stores files in `pb_data/storage` on the local filesystem. For production, you can configure S3-compatible storage (AWS S3, MinIO, Wasabi, DigitalOcean Spaces, etc.) from:
**Dashboard > Settings > Files storage**

This is configured server-side and doesn't require SDK changes.

## Related Documentation

- [Collections](./COLLECTIONS.md) - Collection and field configuration
- [Authentication](./AUTHENTICATION.md) - Required for protected files

