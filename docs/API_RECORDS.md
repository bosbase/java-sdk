# API Records - Java SDK Documentation

## Overview

The Records API provides comprehensive CRUD (Create, Read, Update, Delete) operations for collection records, along with powerful search, filtering, and authentication capabilities.

**Key Features:**
- Paginated list and search with filtering and sorting
- Single record retrieval with expand support
- Create, update, and delete operations
- Batch operations for multiple records
- Authentication methods (password, OAuth2, OTP)
- Email verification and password reset
- Relation expansion up to 6 levels deep
- Field selection and excerpt modifiers

**Backend Endpoints:**
- `GET /api/collections/{collection}/records` - List records
- `GET /api/collections/{collection}/records/{id}` - View record
- `POST /api/collections/{collection}/records` - Create record
- `PATCH /api/collections/{collection}/records/{id}` - Update record
- `DELETE /api/collections/{collection}/records/{id}` - Delete record
- `POST /api/batch` - Batch operations

## CRUD Operations

### List/Search Records

Returns a paginated records list with support for sorting, filtering, and expansion.

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Basic list with pagination
ResultList result = pb.collection("posts").getList(1, 50, false, null, null, null, null, null, null);

System.out.println(result.page);        // 1
System.out.println(result.perPage);     // 50
System.out.println(result.totalItems);  // 150
System.out.println(result.totalPages);  // 3
System.out.println(result.items);       // Array of records
```

#### Advanced List with Filtering and Sorting

```java
// Filter and sort
ResultList result = pb.collection("posts").getList(
    1, 50, false,
    "created >= '2022-01-01 00:00:00' && status = 'published'",
    "-created,title",  // DESC by created, ASC by title
    "author,categories",
    null, null, null, null, null
);

// Filter with operators
ResultList result2 = pb.collection("posts").getList(
    1, 50, false,
    "title ~ 'javascript' && views > 100",
    "-views",
    null, null, null, null, null
);
```

#### Get Full List

Fetch all records at once (useful for small collections):

```java
// Get all records
List<ObjectNode> allPosts = pb.collection("posts").getFullList(
    200,
    "status = 'published'",
    "-created",
    null, null, null, null
);

// With batch size for large collections
List<ObjectNode> allPosts = pb.collection("posts").getFullList(
    200,
    "-created",
    null, null, null, null
);
```

#### Get First Matching Record

Get only the first record that matches a filter:

```java
ObjectNode post = pb.collection("posts").getFirstListItem(
    "slug = 'my-post-slug'",
    "author,categories.tags",
    null, null, null, null, null
);
```

### View Record

Retrieve a single record by ID:

```java
// Basic retrieval
ObjectNode record = pb.collection("posts").getOne("RECORD_ID", null, null, null, null);

// With expanded relations
ObjectNode record = pb.collection("posts").getOne(
    "RECORD_ID",
    "author,categories,tags",
    null, null, null
);

// Nested expand
ObjectNode record = pb.collection("comments").getOne(
    "COMMENT_ID",
    "post.author,user",
    null, null, null
);

// Field selection
ObjectNode record = pb.collection("posts").getOne(
    "RECORD_ID",
    null,
    "id,title,content,author.name",
    null, null
);
```

### Create Record

Create a new record:

```java
// Simple create
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My First Post");
recordData.put("content", "Lorem ipsum...");
recordData.put("status", "draft");

ObjectNode record = pb.collection("posts").create(recordData, null, null, null);

// Create with relations
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Post");
recordData.put("author", "AUTHOR_ID");           // Single relation
recordData.put("categories", List.of("cat1", "cat2"));  // Multiple relation

ObjectNode record = pb.collection("posts").create(recordData, null, null, null);

// Create with file upload (multipart/form-data)
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Post");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "image.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode record = pb.collection("posts").create(recordData, files, null, null);

// Create with expand to get related data immediately
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Post");
recordData.put("author", "AUTHOR_ID");

ObjectNode record = pb.collection("posts").create(recordData, null, "author", null);
```

### Update Record

Update an existing record:

```java
// Simple update
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");
updateData.put("status", "published");

ObjectNode record = pb.collection("posts").update("RECORD_ID", updateData, null, null, null);

// Update with relations
Map<String, Object> updateData = new HashMap<>();
updateData.put("categories+", "NEW_CATEGORY_ID");  // Append
updateData.put("tags-", "OLD_TAG_ID");              // Remove

pb.collection("posts").update("RECORD_ID", updateData, null, null, null);

// Update with file upload
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(newFileBytes, "newimage.jpg", "image/jpeg"));
files.put("image", fileList);

ObjectNode record = pb.collection("posts").update("RECORD_ID", updateData, files, null, null);

// Update with expand
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated");

ObjectNode record = pb.collection("posts").update(
    "RECORD_ID",
    updateData,
    null,
    "author,categories",
    null
);
```

### Delete Record

Delete a record:

```java
// Simple delete
pb.collection("posts").delete("RECORD_ID", null, null);

// Note: Returns 204 No Content on success
// Throws error if record doesn't exist or permission denied
```

## Filter Syntax

The filter parameter supports a powerful query syntax:

### Comparison Operators

```java
// Equal
String filter = "status = 'published'";

// Not equal
String filter = "status != 'draft'";

// Greater than / Less than
String filter = "views > 100";
String filter = "created < '2023-01-01'";

// Greater/Less than or equal
String filter = "age >= 18";
String filter = "price <= 99.99";
```

### String Operators

```java
// Contains (like)
String filter = "title ~ 'javascript'";
// Equivalent to: title LIKE "%javascript%"

// Not contains
String filter = "title !~ 'deprecated'";

// Exact match (case-sensitive)
String filter = "email = 'user@example.com'";
```

### Array Operators (for multiple relations/files)

```java
// Any of / At least one
String filter = "tags.id ?= 'TAG_ID'";         // Any tag matches
String filter = "tags.name ?~ 'important'";    // Any tag name contains "important"

// All must match
String filter = "tags.id = 'TAG_ID' && tags.id = 'TAG_ID2'";
```

### Logical Operators

```java
// AND
String filter = "status = 'published' && views > 100";

// OR
String filter = "status = 'published' || status = 'featured'";

// Parentheses for grouping
String filter = "(status = 'published' || featured = true) && views > 50";
```

## Sorting

Sort records using the `sort` parameter:

```java
// Single field (ASC)
String sort = "created";

// Single field (DESC)
String sort = "-created";

// Multiple fields
String sort = "-created,title";  // DESC by created, then ASC by title

// Supported fields
String sort = "@random";         // Random order
String sort = "@rowid";          // Internal row ID
String sort = "id";              // Record ID
String sort = "fieldName";       // Any collection field

// Relation field sorting
String sort = "author.name";     // Sort by related author's name
```

## Field Selection

Control which fields are returned:

```java
// Specific fields
String fields = "id,title,content";

// All fields at level
String fields = "*";

// Nested field selection
String fields = "*,author.name,author.email";

// Excerpt modifier for text fields
String fields = "*,content:excerpt(200,true)";
// Returns first 200 characters with ellipsis if truncated

// Combined
String fields = "*,content:excerpt(200),author.name,author.email";
```

## Expanding Relations

Expand related records without additional API calls:

```java
// Single relation
String expand = "author";

// Multiple relations
String expand = "author,categories,tags";

// Nested relations (up to 6 levels)
String expand = "author.profile,categories.tags";

// Back-relations
String expand = "comments_via_post.user";
```

See [Relations Documentation](./RELATIONS.md) for detailed information.

## Pagination Options

```java
// Skip total count (faster queries)
ResultList result = pb.collection("posts").getList(
    1, 50, true,  // skipTotal = true
    "status = 'published'",
    null, null, null, null, null
);
// totalItems and totalPages will be -1

// Get Full List with batch processing
List<ObjectNode> allPosts = pb.collection("posts").getFullList(
    200,  // batch size
    "-created",
    null, null, null, null
);
// Processes in batches of 200 to avoid memory issues
```

## Batch Operations

Execute multiple operations in a single transaction:

```java
// Create a batch
BatchService batch = pb.createBatch();

// Add operations
batch.collection("posts").create(Map.of("title", "Post 1", "author", "AUTHOR_ID"), null, null, null);
batch.collection("posts").create(Map.of("title", "Post 2", "author", "AUTHOR_ID"), null, null, null);
batch.collection("tags").update("TAG_ID", Map.of("name", "Updated Tag"), null, null, null);
batch.collection("categories").delete("CAT_ID", null, null);

// Upsert (create or update based on id)
Map<String, Object> upsertData = new HashMap<>();
upsertData.put("id", "EXISTING_ID");
upsertData.put("title", "Updated Post");
batch.collection("posts").update("EXISTING_ID", upsertData, null, null, null);

// Send batch request
List<JsonNode> results = batch.send();

// Results is an array matching the order of operations
for (int i = 0; i < results.size(); i++) {
    JsonNode result = results.get(i);
    // Process result
}
```

**Note**: Batch operations must be enabled in Dashboard > Settings > Application.

## Authentication Actions

### List Auth Methods

Get available authentication methods for a collection:

```java
ObjectNode methods = pb.collection("users").listAuthMethods(null, null, null, null);

System.out.println(methods.path("password").path("enabled").asBoolean());
System.out.println(methods.path("oauth2").path("enabled").asBoolean());
ArrayNode providers = (ArrayNode) methods.path("oauth2").path("providers");
System.out.println(methods.path("otp").path("enabled").asBoolean());
System.out.println(methods.path("mfa").path("enabled").asBoolean());
```

### Auth with Password

```java
ObjectNode authData = pb.collection("users").authWithPassword(
    "user@example.com",  // username or email
    "password123",
    null, null, null, null, null, null, null, null
);

// Auth data is automatically stored in pb.authStore
System.out.println(pb.authStore.isValid());    // true
System.out.println(pb.authStore.getToken());      // JWT token
System.out.println(pb.authStore.getModel().path("id").asText());  // User ID

// Access the returned data
System.out.println(authData.path("token").asText());
System.out.println(authData.path("record"));

// With expand
ObjectNode authData = pb.collection("users").authWithPassword(
    "user@example.com",
    "password123",
    "profile",  // expand
    null, null, null, null, null, null, null
);
```

### Auth with OAuth2

```java
// Step 1: Get OAuth2 URL (usually done in UI)
ObjectNode methods = pb.collection("users").listAuthMethods(null, null, null, null);
ArrayNode providers = (ArrayNode) methods.path("oauth2").path("providers");
// Find provider in array

// Step 2: After redirect, exchange code for token
ObjectNode authData = pb.collection("users").authWithOAuth2Code(
    "google",                    // Provider name
    "AUTHORIZATION_CODE",        // From redirect URL
    "CODE_VERIFIER",             // From step 1
    "https://yourapp.com/callback", // Redirect URL
    Map.of("name", "John Doe"),  // Optional data for new accounts
    null, null, null, null, null, null, null, null
);
```

### Auth with OTP (One-Time Password)

```java
// Step 1: Request OTP
ObjectNode otpRequest = pb.collection("users").requestOTP("user@example.com", null, null, null);
// Returns: { "otpId": "..." }

// Step 2: User enters OTP from email
// Step 3: Authenticate with OTP
ObjectNode authData = pb.collection("users").authWithOTP(
    otpRequest.path("otpId").asText(),
    "123456",  // OTP from email
    null, null, null, null, null, null, null, null
);
```

### Auth Refresh

Refresh the current auth token and get updated user data:

```java
// Refresh auth (useful on page reload)
ObjectNode authData = pb.collection("users").authRefresh(null, null, null, null);

// Check if still valid
if (pb.authStore.isValid()) {
    System.out.println("User is authenticated");
} else {
    System.out.println("Token expired or invalid");
}
```

### Email Verification

```java
// Request verification email
pb.collection("users").requestVerification("user@example.com", null, null, null);

// Confirm verification (on verification page)
pb.collection("users").confirmVerification("VERIFICATION_TOKEN", null, null, null);
```

### Password Reset

```java
// Request password reset email
pb.collection("users").requestPasswordReset("user@example.com", null, null, null);

// Confirm password reset (on reset page)
// Note: This invalidates all previous auth tokens
pb.collection("users").confirmPasswordReset(
    "RESET_TOKEN",
    "newpassword123",
    "newpassword123",  // Confirm
    null, null, null
);
```

### Email Change

```java
// Must be authenticated first
pb.collection("users").authWithPassword("user@example.com", "password", null, null, null, null, null, null, null, null);

// Request email change
pb.collection("users").requestEmailChange("newemail@example.com", null, null, null);

// Confirm email change (on confirmation page)
// Note: This invalidates all previous auth tokens
pb.collection("users").confirmEmailChange(
    "EMAIL_CHANGE_TOKEN",
    "currentpassword",
    null, null, null
);
```

## Complete Examples

### Example 1: Blog Post Search with Filters

```java
public List<ObjectNode> searchPosts(String query, String categoryId, Integer minViews) {
    StringBuilder filter = new StringBuilder();
    filter.append("title ~ '").append(query).append("' || content ~ '").append(query).append("'");
    
    if (categoryId != null) {
        filter.append(" && categories.id ?= '").append(categoryId).append("'");
    }
    
    if (minViews != null) {
        filter.append(" && views >= ").append(minViews);
    }
    
    ResultList result = pb.collection("posts").getList(
        1, 20, false,
        filter.toString(),
        "-created",
        "author,categories",
        null, null, null, null, null
    );
    
    return result.items;
}
```

### Example 2: User Dashboard with Related Content

```java
public Map<String, List<ObjectNode>> getUserDashboard(String userId) {
    // Get user's posts
    ResultList posts = pb.collection("posts").getList(
        1, 10, false,
        "author = '" + userId + "'",
        "-created",
        "categories",
        null, null, null, null, null
    );
    
    // Get user's comments
    ResultList comments = pb.collection("comments").getList(
        1, 10, false,
        "user = '" + userId + "'",
        "-created",
        "post",
        null, null, null, null, null
    );
    
    Map<String, List<ObjectNode>> dashboard = new HashMap<>();
    dashboard.put("posts", posts.items);
    dashboard.put("comments", comments.items);
    return dashboard;
}
```

### Example 3: Advanced Filtering

```java
// Complex filter example
String filter = "(status = 'published' || featured = true) && " +
                "created >= '2023-01-01' && " +
                "(tags.id ?= 'important' || categories.id = 'news') && " +
                "views > 100 && " +
                "author.email != ''";

ResultList result = pb.collection("posts").getList(
    1, 50, false,
    filter,
    "-views,created",
    "author.profile,tags,categories",
    "*,content:excerpt(300),author.name,author.email",
    null, null, null, null
);
```

### Example 4: Batch Create Posts

```java
public List<JsonNode> createMultiplePosts(List<Map<String, Object>> postsData) {
    BatchService batch = pb.createBatch();
    
    for (Map<String, Object> postData : postsData) {
        batch.collection("posts").create(postData, null, null, null);
    }
    
    List<JsonNode> results = batch.send();
    
    // Check for failures
    for (int i = 0; i < results.size(); i++) {
        JsonNode result = results.get(i);
        // Process result
    }
    
    return results;
}
```

### Example 5: Pagination Helper

```java
public List<ObjectNode> getAllRecordsPaginated(String collectionName, Map<String, Object> options) {
    List<ObjectNode> allRecords = new ArrayList<>();
    int page = 1;
    boolean hasMore = true;
    
    while (hasMore) {
        ResultList result = pb.collection(collectionName).getList(
            page, 500, true,  // skipTotal for performance
            (String) options.get("filter"),
            (String) options.get("sort"),
            (String) options.get("expand"),
            null, null, null, null, null
        );
        
        allRecords.addAll(result.items);
        
        hasMore = result.items.size() == 500;
        page++;
    }
    
    return allRecords;
}
```

## Error Handling

```java
try {
    Map<String, Object> recordData = new HashMap<>();
    recordData.put("title", "My Post");
    ObjectNode record = pb.collection("posts").create(recordData, null, null, null);
} catch (ClientResponseError error) {
    if (error.status == 400) {
        // Validation error
        System.err.println("Validation errors: " + error.response);
    } else if (error.status == 403) {
        // Permission denied
        System.err.println("Access denied");
    } else if (error.status == 404) {
        // Not found
        System.err.println("Collection or record not found");
    } else {
        System.err.println("Unexpected error: " + error);
    }
}
```

## Best Practices

1. **Use Pagination**: Always use pagination for large datasets
2. **Skip Total When Possible**: Use `skipTotal: true` for better performance when you don't need counts
3. **Batch Operations**: Use batch for multiple operations to reduce round trips
4. **Field Selection**: Only request fields you need to reduce payload size
5. **Expand Wisely**: Only expand relations you actually use
6. **Filter Before Sort**: Apply filters before sorting for better performance
7. **Cache Auth Tokens**: Auth tokens are automatically stored in `authStore`, no need to manually cache
8. **Handle Errors**: Always handle authentication and permission errors gracefully

## Related Documentation

- [Collections](./COLLECTIONS.md) - Collection configuration
- [Relations](./RELATIONS.md) - Working with relations
- [API Rules and Filters](./API_RULES_AND_FILTERS.md) - Filter syntax details
- [Authentication](./AUTHENTICATION.md) - Detailed authentication guide
- [Files](./FILES.md) - File uploads and handling

