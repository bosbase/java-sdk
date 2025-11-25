# File API - Java SDK Documentation

## Overview

The File API provides endpoints for downloading and accessing files stored in collection records. It supports thumbnail generation for images, protected file access with tokens, and force download options.

**Key Features:**
- Download files from collection records
- Generate thumbnails for images (crop, fit, resize)
- Protected file access with short-lived tokens
- Force download option for any file type
- Automatic content-type detection
- Support for Range requests and caching

**Backend Endpoints:**
- `GET /api/files/{collection}/{recordId}/{filename}` - Download/fetch file
- `POST /api/files/token` - Generate protected file token

## Download / Fetch File

Downloads a single file resource from a record.

### Basic Usage

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Get a record with a file field
ObjectNode record = pb.collection("posts").getOne("RECORD_ID", null, null, null, null);

// Get the file URL
String fileUrl = pb.files.getURL(record, record.get("image").asText());

// Use in your application
System.out.println("File URL: " + fileUrl);
```

### File URL Structure

The file URL follows this pattern:
```
/api/files/{collectionIdOrName}/{recordId}/{filename}
```

Example:
```
http://127.0.0.1:8090/api/files/posts/abc123/photo_xyz789.jpg
```

## Thumbnails

Generate thumbnails for image files on-the-fly.

### Thumbnail Formats

The following thumbnail formats are supported:

| Format | Example | Description |
|--------|---------|-------------|
| `WxH` | `100x300` | Crop to WxH viewbox (from center) |
| `WxHt` | `100x300t` | Crop to WxH viewbox (from top) |
| `WxHb` | `100x300b` | Crop to WxH viewbox (from bottom) |
| `WxHf` | `100x300f` | Fit inside WxH viewbox (without cropping) |
| `0xH` | `0x300` | Resize to H height preserving aspect ratio |
| `Wx0` | `100x0` | Resize to W width preserving aspect ratio |

### Using Thumbnails

```java
// Get thumbnail URL
Map<String, String> options = new HashMap<>();
options.put("thumb", "100x100");
String thumbUrl = pb.files.getURL(record, record.get("image").asText(), options);

// Different thumbnail sizes
Map<String, String> smallOptions = new HashMap<>();
smallOptions.put("thumb", "50x50");
String smallThumb = pb.files.getURL(record, record.get("image").asText(), smallOptions);

Map<String, String> mediumOptions = new HashMap<>();
mediumOptions.put("thumb", "200x200");
String mediumThumb = pb.files.getURL(record, record.get("image").asText(), mediumOptions);

Map<String, String> largeOptions = new HashMap<>();
largeOptions.put("thumb", "500x500");
String largeThumb = pb.files.getURL(record, record.get("image").asText(), largeOptions);

// Fit thumbnail (no cropping)
Map<String, String> fitOptions = new HashMap<>();
fitOptions.put("thumb", "200x200f");
String fitThumb = pb.files.getURL(record, record.get("image").asText(), fitOptions);

// Resize to specific width
Map<String, String> widthOptions = new HashMap<>();
widthOptions.put("thumb", "300x0");
String widthThumb = pb.files.getURL(record, record.get("image").asText(), widthOptions);

// Resize to specific height
Map<String, String> heightOptions = new HashMap<>();
heightOptions.put("thumb", "0x200");
String heightThumb = pb.files.getURL(record, record.get("image").asText(), heightOptions);
```

### Thumbnail Behavior

- **Image Files Only**: Thumbnails are only generated for image files (PNG, JPG, JPEG, GIF, WEBP)
- **Non-Image Files**: For non-image files, the thumb parameter is ignored and the original file is returned
- **Caching**: Thumbnails are cached and reused if already generated
- **Fallback**: If thumbnail generation fails, the original file is returned
- **Field Configuration**: Thumb sizes must be defined in the file field's `thumbs` option or use default `100x100`

## Protected Files

Protected files require a special token for access, even if you're authenticated.

### Getting a File Token

```java
// Must be authenticated first
pb.collection("users").authWithPassword("user@example.com", "password", null, null, null, null, null, null, null, null);

// Get file token
String token = pb.files.getToken();

System.out.println("Token: " + token); // Short-lived JWT token
```

### Using Protected File Token

```java
// Get protected file URL with token
Map<String, String> options = new HashMap<>();
options.put("token", token);
String protectedFileUrl = pb.files.getURL(record, record.get("document").asText(), options);

// Access the file (e.g., download or display)
System.out.println("Protected file URL: " + protectedFileUrl);
```

### Protected File Example

```java
public String displayProtectedImage(String recordId) throws Exception {
    // Authenticate
    pb.collection("users").authWithPassword("user@example.com", "password", null, null, null, null, null, null, null, null);
    
    // Get record
    ObjectNode record = pb.collection("documents").getOne(recordId, null, null, null, null);
    
    // Get file token
    String token = pb.files.getToken();
    
    // Get protected file URL
    Map<String, String> options = new HashMap<>();
    options.put("token", token);
    options.put("thumb", "300x300");
    String imageUrl = pb.files.getURL(record, record.get("thumbnail").asText(), options);
    
    return imageUrl;
}
```

### Token Lifetime

- File tokens are short-lived (typically expires after a few minutes)
- Tokens are associated with the authenticated user/superuser
- Generate a new token if the previous one expires

## Force Download

Force files to download instead of being displayed in the browser.

```java
// Force download
Map<String, String> options = new HashMap<>();
options.put("download", "1");
String downloadUrl = pb.files.getURL(record, record.get("document").asText(), options);

System.out.println("Download URL: " + downloadUrl);
```

### Download Parameter Values

```java
// These all force download:
Map<String, String> options1 = new HashMap<>();
options1.put("download", "1");
String url1 = pb.files.getURL(record, filename, options1);

// These allow inline display (default):
Map<String, String> options2 = new HashMap<>();
options2.put("download", "0");
String url2 = pb.files.getURL(record, filename, options2);

// Or no download parameter
String url3 = pb.files.getURL(record, filename);
```

## Complete Examples

### Example 1: Image Gallery

```java
public void displayImageGallery(String recordId) throws Exception {
    ObjectNode record = pb.collection("posts").getOne(recordId, null, null, null, null);
    
    List<String> images = new ArrayList<>();
    JsonNode imagesNode = record.get("images");
    if (imagesNode.isArray()) {
        for (JsonNode img : imagesNode) {
            images.add(img.asText());
        }
    } else if (imagesNode != null) {
        images.add(imagesNode.asText());
    }
    
    for (String filename : images) {
        // Thumbnail for gallery
        Map<String, String> thumbOptions = new HashMap<>();
        thumbOptions.put("thumb", "200x200");
        String thumbUrl = pb.files.getURL(record, filename, thumbOptions);
        
        // Full image URL
        String fullUrl = pb.files.getURL(record, filename);
        
        System.out.println("Thumbnail: " + thumbUrl);
        System.out.println("Full: " + fullUrl);
    }
}
```

### Example 2: File Download Handler

```java
public String downloadFile(String recordId, String filename) throws Exception {
    ObjectNode record = pb.collection("documents").getOne(recordId, null, null, null, null);
    
    // Get download URL
    Map<String, String> options = new HashMap<>();
    options.put("download", "1");
    String downloadUrl = pb.files.getURL(record, filename, options);
    
    return downloadUrl;
}
```

### Example 3: Protected File Viewer

```java
public String viewProtectedFile(String recordId) throws Exception {
    // Authenticate
    if (!pb.authStore.isValid()) {
        pb.collection("users").authWithPassword("user@example.com", "password", null, null, null, null, null, null, null, null);
    }
    
    // Get record
    ObjectNode record = pb.collection("private_docs").getOne(recordId, null, null, null, null);
    
    // Get token
    String token;
    try {
        token = pb.files.getToken();
    } catch (Exception e) {
        System.err.println("Failed to get file token: " + e.getMessage());
        return null;
    }
    
    // Get file URL
    Map<String, String> options = new HashMap<>();
    options.put("token", token);
    String fileUrl = pb.files.getURL(record, record.get("file").asText(), options);
    
    return fileUrl;
}
```

### Example 4: Multiple Files with Thumbnails

```java
public void displayFileList(String recordId) throws Exception {
    ObjectNode record = pb.collection("attachments").getOne(recordId, null, null, null, null);
    
    List<String> files = new ArrayList<>();
    JsonNode filesNode = record.get("files");
    if (filesNode.isArray()) {
        for (JsonNode file : filesNode) {
            files.add(file.asText());
        }
    }
    
    for (String filename : files) {
        // Check if it's an image
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        boolean isImage = List.of("jpg", "jpeg", "png", "gif", "webp").contains(ext);
        
        if (isImage) {
            // Show thumbnail
            Map<String, String> thumbOptions = new HashMap<>();
            thumbOptions.put("thumb", "100x100");
            String thumbUrl = pb.files.getURL(record, filename, thumbOptions);
            System.out.println("Thumbnail: " + thumbUrl);
        }
        
        // File download link
        Map<String, String> downloadOptions = new HashMap<>();
        downloadOptions.put("download", "1");
        String downloadUrl = pb.files.getURL(record, filename, downloadOptions);
        System.out.println("Download: " + downloadUrl);
    }
}
```

### Example 5: Image Upload Preview with Thumbnail

```java
public String previewUploadedImage(ObjectNode record, String filename) {
    // Get thumbnail for preview
    Map<String, String> options = new HashMap<>();
    options.put("thumb", "200x200f");  // Fit to 200x200 without cropping
    String previewUrl = pb.files.getURL(record, filename, options);
    
    return previewUrl;
}
```

## Error Handling

```java
try {
    String fileUrl = pb.files.getURL(record, record.get("image").asText());
    
    // Verify URL is valid
    if (fileUrl == null || fileUrl.isEmpty()) {
        throw new RuntimeException("Invalid file URL");
    }
    
    System.out.println("File URL: " + fileUrl);
    
} catch (Exception e) {
    System.err.println("File access error: " + e.getMessage());
}
```

### Protected File Token Error Handling

```java
public String getProtectedFileUrl(ObjectNode record, String filename) {
    try {
        // Get token
        String token = pb.files.getToken();
        
        // Get file URL
        Map<String, String> options = new HashMap<>();
        options.put("token", token);
        return pb.files.getURL(record, filename, options);
        
    } catch (Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("401")) {
            System.err.println("Not authenticated");
            // Redirect to login
        } else if (e.getMessage() != null && e.getMessage().contains("403")) {
            System.err.println("No permission to access file");
        } else {
            System.err.println("Failed to get file token: " + e.getMessage());
        }
        return null;
    }
}
```

## Best Practices

1. **Use Thumbnails for Lists**: Use thumbnails when displaying images in lists/grids to reduce bandwidth
2. **Lazy Loading**: Consider lazy loading for images below the fold
3. **Cache Tokens**: Store file tokens and reuse them until they expire
4. **Error Handling**: Always handle file loading errors gracefully
5. **Content-Type**: Let the server handle content-type detection automatically
6. **Range Requests**: The API supports Range requests for efficient video/audio streaming
7. **Caching**: Files are cached with a 30-day cache-control header
8. **Security**: Always use tokens for protected files, never expose them in client-side code

## Thumbnail Size Guidelines

| Use Case | Recommended Size |
|----------|-----------------|
| Profile picture | `100x100` or `150x150` |
| List thumbnails | `200x200` or `300x300` |
| Card images | `400x400` or `500x500` |
| Gallery previews | `300x300f` (fit) or `400x400f` |
| Hero images | Use original or `800x800f` |
| Avatar | `50x50` or `75x75` |

## Limitations

- **Thumbnails**: Only work for image files (PNG, JPG, JPEG, GIF, WEBP)
- **Protected Files**: Require authentication to get tokens
- **Token Expiry**: File tokens expire after a short period (typically minutes)
- **File Size**: Large files may take time to generate thumbnails on first request
- **Thumb Sizes**: Must match sizes defined in field configuration or use default `100x100`

## Related Documentation

- [Files Upload and Handling](./FILES.md) - Uploading and managing files
- [API Records](./API_RECORDS.md) - Working with records
- [Collections](./COLLECTIONS.md) - Collection configuration

