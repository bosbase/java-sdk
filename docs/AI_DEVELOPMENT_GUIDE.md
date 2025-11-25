# AI Development Guide - Java SDK

This guide provides a comprehensive, fast reference for AI systems to quickly develop applications using the BosBase Java SDK. All examples are production-ready and follow best practices.

## Table of Contents

1. [Authentication](#authentication)
2. [Initialize Collections](#initialize-collections)
3. [Define Collection Fields](#define-collection-fields)
4. [Add Data to Collections](#add-data-to-collections)
5. [Modify Collection Data](#modify-collection-data)
6. [Delete Data from Collections](#delete-data-from-collections)
7. [Query Collection Contents](#query-collection-contents)
8. [Add and Delete Fields from Collections](#add-and-delete-fields-from-collections)
9. [Query Collection Field Information](#query-collection-field-information)
10. [Upload Files](#upload-files)
11. [Query Logs](#query-logs)
12. [Send Emails](#send-emails)

---

## Authentication

### Initialize Client

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://localhost:8090");
```

### Password Authentication

```java
// Authenticate with email/username and password
ObjectNode authData = pb.collection("users").authWithPassword(
    "user@example.com",
    "password123",
    null, null, null, null, null, null, null, null
);

// Auth data is automatically stored
System.out.println(pb.authStore.isValid());  // true
System.out.println(pb.authStore.getToken()); // JWT token
System.out.println(pb.authStore.getModel());  // User record
```

### OAuth2 Authentication

```java
// Get OAuth2 providers
ObjectNode methods = pb.collection("users").listAuthMethods(null, null, null, null);
ArrayNode providers = (ArrayNode) methods.path("oauth2").path("providers");
// Available providers

// Authenticate with OAuth2
ObjectNode authData = pb.collection("users").authWithOAuth2(
    "google",
    null, null, null, null, null, null, null, null, null, null, null, null
);
```

### OTP Authentication

```java
// Request OTP
ObjectNode otpResponse = pb.collection("users").requestOTP("user@example.com", null, null, null);

// Authenticate with OTP
ObjectNode authData = pb.collection("users").authWithOTP(
    otpResponse.path("otpId").asText(),
    "123456", // OTP code
    null, null, null, null, null, null, null, null
);
```

### Check Authentication Status

```java
if (pb.authStore.isValid()) {
    ObjectNode user = pb.authStore.getModel();
    System.out.println("Authenticated as: " + user.path("email").asText());
} else {
    System.out.println("Not authenticated");
}
```

### Logout

```java
pb.authStore.clear();
```

---

## Initialize Collections

### Create Base Collection

```java
Map<String, Object> collectionData = new HashMap<>();
collectionData.put("name", "posts");
collectionData.put("type", "base");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
fields.add(titleField);

collectionData.put("fields", fields);

ObjectNode collection = pb.collections.create(collectionData, null, null, null);
System.out.println("Collection ID: " + collection.path("id").asText());
```

### Create Auth Collection

```java
Map<String, Object> authCollectionData = new HashMap<>();
authCollectionData.put("name", "users");
authCollectionData.put("type", "auth");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> nameField = new HashMap<>();
nameField.put("name", "name");
nameField.put("type", "text");
nameField.put("required", false);
fields.add(nameField);

authCollectionData.put("fields", fields);

Map<String, Object> passwordAuth = new HashMap<>();
passwordAuth.put("enabled", true);
passwordAuth.put("identityFields", List.of("email", "username"));
authCollectionData.put("passwordAuth", passwordAuth);

ObjectNode authCollection = pb.collections.create(authCollectionData, null, null, null);
```

### Create View Collection

```java
Map<String, Object> viewData = new HashMap<>();
viewData.put("name", "published_posts");
viewData.put("type", "view");
viewData.put("viewQuery", "SELECT * FROM posts WHERE published = true");

ObjectNode viewCollection = pb.collections.create(viewData, null, null, null);
```

### Get Collection by ID or Name

```java
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
// or by ID
ObjectNode collection = pb.collections.getOne("_pbc_2287844090", null, null, null, null);
```

---

## Define Collection Fields

### Add Field to Collection

```java
Map<String, Object> field = new HashMap<>();
field.put("name", "content");
field.put("type", "editor");
field.put("required", false);

ObjectNode updatedCollection = pb.collections.addField("posts", field, null);
```

### Common Field Types

```java
// Text field
Map<String, Object> textField = new HashMap<>();
textField.put("name", "title");
textField.put("type", "text");
textField.put("required", true);
textField.put("min", 10);
textField.put("max", 255);

// Number field
Map<String, Object> numberField = new HashMap<>();
numberField.put("name", "views");
numberField.put("type", "number");
numberField.put("required", false);
numberField.put("min", 0);

// Boolean field
Map<String, Object> boolField = new HashMap<>();
boolField.put("name", "published");
boolField.put("type", "bool");
boolField.put("required", false);

// Date field
Map<String, Object> dateField = new HashMap<>();
dateField.put("name", "published_at");
dateField.put("type", "date");
dateField.put("required", false);

// File field
Map<String, Object> fileField = new HashMap<>();
fileField.put("name", "avatar");
fileField.put("type", "file");
fileField.put("required", false);
fileField.put("maxSelect", 1);
fileField.put("maxSize", 2097152); // 2MB
fileField.put("mimeTypes", List.of("image/jpeg", "image/png"));

// Relation field
Map<String, Object> relationField = new HashMap<>();
relationField.put("name", "author");
relationField.put("type", "relation");
relationField.put("required", true);
relationField.put("collectionId", "_pbc_users_auth_");
relationField.put("maxSelect", 1);

// Select field
Map<String, Object> selectField = new HashMap<>();
selectField.put("name", "status");
selectField.put("type", "select");
selectField.put("required", true);
Map<String, Object> options = new HashMap<>();
options.put("values", List.of("draft", "published", "archived"));
selectField.put("options", options);
```

### Update Field

```java
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
// Update field configuration
// ... modify field in collection object
pb.collections.update("posts", JsonUtils.jsonNodeToMap(collection), null, null);
```

### Remove Field

```java
ObjectNode updatedCollection = pb.collections.removeField("posts", "old_field", null);
```

---

## Add Data to Collections

### Create Single Record

```java
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My First Post");
recordData.put("content", "This is the content");
recordData.put("published", true);

ObjectNode record = pb.collection("posts").create(recordData, null, null, null);
System.out.println("Created record ID: " + record.path("id").asText());
```

### Create Record with File Upload

```java
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "Post with Image");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(imageBytes, "image.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode record = pb.collection("posts").create(recordData, files, null, null);
```

### Create Record with Relations

```java
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Post");
recordData.put("author", "user_record_id"); // Related record ID
recordData.put("categories", List.of("cat1_id", "cat2_id")); // Multiple relations

ObjectNode record = pb.collection("posts").create(recordData, null, null, null);
```

### Batch Create Records

```java
BatchService batch = pb.createBatch();
batch.collection("posts").create(Map.of("title", "Post 1"), null, null, null);
batch.collection("posts").create(Map.of("title", "Post 2"), null, null, null);
List<JsonNode> results = batch.send();
```

---

## Modify Collection Data

### Update Single Record

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");
updateData.put("content", "Updated content");

ObjectNode updated = pb.collection("posts").update("record_id", updateData, null, null, null);
```

### Update Record with File

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(newFileBytes, "newimage.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode updated = pb.collection("posts").update("record_id", updateData, files, null, null);
```

### Partial Update

```java
// Only update specific fields
Map<String, Object> updateData = new HashMap<>();
updateData.put("views", 100); // Only update views

ObjectNode updated = pb.collection("posts").update("record_id", updateData, null, null, null);
```

---

## Delete Data from Collections

### Delete Single Record

```java
pb.collection("posts").delete("record_id", null, null);
```

### Delete Multiple Records

```java
// Using batch
BatchService batch = pb.createBatch();
batch.collection("posts").delete("record_id_1", null, null);
batch.collection("posts").delete("record_id_2", null, null);
batch.send();
```

### Delete All Records (Truncate)

```java
pb.collections.truncate("posts", null);
```

---

## Query Collection Contents

### List Records with Pagination

```java
ResultList result = pb.collection("posts").getList(1, 50, false, null, null, null, null, null, null);

System.out.println(result.page);        // 1
System.out.println(result.perPage);     // 50
System.out.println(result.totalItems); // Total count
System.out.println(result.items);      // Array of records
```

### Filter Records

```java
ResultList result = pb.collection("posts").getList(
    1, 50, false,
    "published = true && views > 100",
    "-created",
    null, null, null, null, null
);
```

### Filter Operators

```java
// Equality
String filter = "status = 'published'";

// Comparison
String filter = "views > 100";
String filter = "created >= '2023-01-01'";

// Text search
String filter = "title ~ 'javascript'";

// Multiple conditions
String filter = "status = 'published' && views > 100";
String filter = "status = 'draft' || status = 'pending'";

// Relation filter
String filter = "author.id = 'user_id'";
```

### Sort Records

```java
// Single field
String sort = "-created";  // DESC
String sort = "title";     // ASC

// Multiple fields
String sort = "-created,title";  // DESC by created, then ASC by title
```

### Expand Relations

```java
ResultList result = pb.collection("posts").getList(
    1, 50, false,
    null,
    null,
    "author,categories",
    null, null, null, null, null
);

// Access expanded data
for (JsonNode post : result.items) {
    System.out.println(post.path("expand").path("author").path("name").asText());
}
```

### Get Single Record

```java
ObjectNode record = pb.collection("posts").getOne("record_id", "author", null, null, null);
```

### Get First Matching Record

```java
ObjectNode record = pb.collection("posts").getFirstListItem(
    "slug = 'my-post-slug'",
    "author",
    null, null, null, null, null
);
```

### Get All Records

```java
List<ObjectNode> allRecords = pb.collection("posts").getFullList(
    200,
    "published = true",
    "-created",
    null, null, null, null
);
```

---

## Add and Delete Fields from Collections

### Add Field

```java
Map<String, Object> field = new HashMap<>();
field.put("name", "tags");
field.put("type", "select");
Map<String, Object> options = new HashMap<>();
options.put("values", List.of("tech", "science", "art"));
field.put("options", options);

ObjectNode collection = pb.collections.addField("posts", field, null);
```

### Update Field

```java
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
// Modify field in collection
pb.collections.update("posts", JsonUtils.jsonNodeToMap(collection), null, null);
```

### Remove Field

```java
ObjectNode collection = pb.collections.removeField("posts", "old_field", null);
```

### Get Field Information

```java
ObjectNode field = pb.collections.getField("posts", "title", null);
System.out.println(field.path("type").asText());
System.out.println(field.path("required").asBoolean());
```

---

## Query Collection Field Information

### Get All Fields for a Collection

```java
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
ArrayNode fields = (ArrayNode) collection.path("fields");
for (JsonNode field : fields) {
    System.out.println(field.path("name").asText() + " " + 
                      field.path("type").asText() + " " + 
                      field.path("required").asBoolean());
}
```

### Get Collection Schema (Simplified)

```java
ObjectNode schema = pb.collections.getSchema("posts", null);
ArrayNode fields = (ArrayNode) schema.path("fields");
// Array of field info
```

### Get All Collection Schemas

```java
ObjectNode schemas = pb.collections.getAllSchemas(null);
ArrayNode collections = (ArrayNode) schemas.path("collections");
for (JsonNode collection : collections) {
    System.out.println(collection.path("name").asText());
}
```

---

## Upload Files

### Upload File with Record Creation

```java
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "Post Title");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "image.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode record = pb.collection("posts").create(recordData, files, null, null);
```

### Upload File with Record Update

```java
Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(newFileBytes, "newimage.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode updated = pb.collection("posts").update("record_id", null, files, null, null);
```

### Get File URL

```java
ObjectNode record = pb.collection("posts").getOne("record_id", null, null, null, null);
String fileUrl = pb.files.getURL(record, record.path("image").asText());
```

### Get File URL with Options

```java
Map<String, Object> options = new HashMap<>();
options.put("thumb", "100x100");  // Thumbnail
options.put("download", true);    // Force download

String fileUrl = pb.files.getURL(record, record.path("image").asText(), options);
```

### Get Private File Token

```java
// For accessing private files
String token = pb.files.getToken(null);
// Use token in file URL query params
```

---

## Query Logs

### List Logs

```java
ResultList logs = pb.logs.getList(1, 50, false, null, null, null, null, null, null);
for (JsonNode log : logs.items) {
    // Process log entries
}
```

### Filter Logs

```java
ResultList logs = pb.logs.getList(
    1, 50, false,
    "level >= 400", // Error level and above
    "-created",
    null, null, null, null, null
);
```

### Get Single Log

```java
ObjectNode log = pb.logs.getOne("log_id", null, null, null, null);
System.out.println(log.path("message").asText());
```

### Get Log Statistics

```java
List<ObjectNode> stats = pb.logs.getStats("level >= 400", null);
for (ObjectNode stat : stats) {
    System.out.println(stat.path("date").asText() + " " + stat.path("total").asInt());
}
```

### Log Levels

- `0` - Debug
- `1` - Info
- `2` - Warning
- `3` - Error
- `4` - Fatal

---

## Send Emails

**Note**: Email sending is typically handled server-side via hooks or backend code. The SDK doesn't provide direct email sending methods, but you can trigger email-related operations.

### Trigger Email Verification

```java
// Request verification email
pb.collection("users").requestVerification("user@example.com", null, null, null);
```

### Trigger Password Reset Email

```java
// Request password reset email
pb.collection("users").requestPasswordReset("user@example.com", null, null, null);
```

### Email Change Request

```java
// Request email change
pb.collection("users").requestEmailChange("newemail@example.com", null, null, null);
```

---

## Complete Example: Full Application Flow

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

public class Example {
    public static void main(String[] args) {
        BosBase pb = new BosBase("http://localhost:8090");
        
        try {
            // 1. Authenticate
            pb.collection("users").authWithPassword(
                "admin@example.com", "password",
                null, null, null, null, null, null, null, null
            );
            
            // 2. Create collection
            Map<String, Object> collectionData = new HashMap<>();
            collectionData.put("name", "posts");
            collectionData.put("type", "base");
            
            List<Map<String, Object>> fields = new ArrayList<>();
            Map<String, Object> titleField = new HashMap<>();
            titleField.put("name", "title");
            titleField.put("type", "text");
            titleField.put("required", true);
            fields.add(titleField);
            
            collectionData.put("fields", fields);
            ObjectNode collection = pb.collections.create(collectionData, null, null, null);
            
            // 3. Add more fields
            Map<String, Object> viewsField = new HashMap<>();
            viewsField.put("name", "views");
            viewsField.put("type", "number");
            viewsField.put("min", 0);
            pb.collections.addField("posts", viewsField, null);
            
            // 4. Create records
            Map<String, Object> postData = new HashMap<>();
            postData.put("title", "Hello World");
            postData.put("content", "My first post");
            postData.put("published", true);
            postData.put("views", 0);
            ObjectNode post = pb.collection("posts").create(postData, null, null, null);
            
            // 5. Query records
            ResultList posts = pb.collection("posts").getList(
                1, 10, false,
                "published = true",
                "-created",
                null, null, null, null, null
            );
            
            // 6. Update record
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("views", 100);
            pb.collection("posts").update(post.path("id").asText(), updateData, null, null, null);
            
            // 7. Query logs
            ResultList logs = pb.logs.getList(
                1, 20, false,
                "level >= 400",
                null, null, null, null, null
            );
            
            System.out.println("Application setup complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Quick Reference

### Common Patterns

```java
// Check if authenticated
if (pb.authStore.isValid()) {
    // ...
}

// Get current user
ObjectNode user = pb.authStore.getModel();

// Refresh auth token
pb.collection("users").authRefresh(null, null, null, null);

// Error handling
try {
    pb.collection("posts").create(Map.of("title", "Test"), null, null, null);
} catch (ClientResponseError err) {
    if (err.status == 400) {
        System.err.println("Validation error: " + err.response);
    } else if (err.status == 401) {
        System.err.println("Not authenticated");
    }
}
```

### Field Types Reference

- `text` - Text input
- `number` - Numeric value
- `bool` - Boolean
- `email` - Email address
- `url` - URL
- `date` - Date
- `select` - Single select
- `json` - JSON data
- `file` - File upload
- `relation` - Relation to another collection
- `editor` - Rich text editor

---

## Best Practices

1. **Always handle errors**: Wrap API calls in try-catch
2. **Check authentication**: Verify `pb.authStore.isValid()` before operations
3. **Use pagination**: Don't fetch all records at once for large collections
4. **Validate data**: Ensure required fields are provided
5. **Use filters**: Filter data on the server, not client-side
6. **Expand relations wisely**: Only expand what you need
7. **Handle file uploads**: Use FileAttachment for file fields
8. **Refresh tokens**: Use `authRefresh()` to maintain sessions

---

## LangChaingo Recipes

### Quick Completion

```java
Map<String, Object> completionData = new HashMap<>();
Map<String, Object> model = new HashMap<>();
model.put("provider", "openai");
model.put("model", "gpt-4o-mini");
completionData.put("model", model);

List<Map<String, Object>> messages = new ArrayList<>();
Map<String, Object> systemMsg = new HashMap<>();
systemMsg.put("role", "system");
systemMsg.put("content", "Answer with one concise line.");
messages.add(systemMsg);

Map<String, Object> userMsg = new HashMap<>();
userMsg.put("role", "user");
userMsg.put("content", "Give me a fun fact about Mars.");
messages.add(userMsg);

completionData.put("messages", messages);
completionData.put("temperature", 0.4);

ObjectNode result = pb.langchaingo.completions(completionData, null);
System.out.println(result.path("content").asText());
```

### Retrieval-Augmented Answering

```java
Map<String, Object> ragData = new HashMap<>();
ragData.put("collection", "knowledge-base");
ragData.put("question", "Why is the sky blue?");
ragData.put("topK", 3);
ragData.put("returnSources", true);

ObjectNode rag = pb.langchaingo.rag(ragData, null);
System.out.println(rag.path("answer").asText());
ArrayNode sources = (ArrayNode) rag.path("sources");
for (JsonNode source : sources) {
    System.out.println(source.path("metadata").path("title").asText());
}
```

---

This guide provides all essential operations for building applications with the BosBase Java SDK. For more detailed information, refer to the specific API documentation files.

