# Collections - Java SDK Documentation

This document provides comprehensive documentation for working with Collections and Fields in the BosBase Java SDK. This documentation is designed to be AI-readable and includes practical examples for all operations.

## Table of Contents

- [Overview](#overview)
- [Collection Types](#collection-types)
- [Collections API](#collections-api)
- [Records API](#records-api)
- [Field Types](#field-types)
- [Examples](#examples)

## Overview

**Collections** represent your application data. Under the hood they are backed by plain SQLite tables that are generated automatically with the collection **name** and **fields** (columns).

A single entry of a collection is called a **record** (a single row in the SQL table).

You can manage your **collections** from the Dashboard, or with the Java SDK using the `collections` service.

Similarly, you can manage your **records** from the Dashboard, or with the Java SDK using the `collection(name)` method which returns a `RecordService` instance.

## Collection Types

Currently there are 3 collection types: **Base**, **View** and **Auth**.

### Base Collection

**Base collection** is the default collection type and it could be used to store any application data (articles, products, posts, etc.).

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Create a base collection
Map<String, Object> collectionData = new HashMap<>();
collectionData.put("name", "articles");
collectionData.put("type", "base");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
titleField.put("min", 6);
titleField.put("max", 100);
fields.add(titleField);

Map<String, Object> descriptionField = new HashMap<>();
descriptionField.put("name", "description");
descriptionField.put("type", "text");
fields.add(descriptionField);

collectionData.put("fields", fields);
ObjectNode collection = pb.collections.create(collectionData, null, null, null);
```

### View Collection

**View collection** is a read-only collection type where the data is populated from a plain SQL `SELECT` statement, allowing users to perform aggregations or any other custom queries.

For example, the following query will create a read-only collection with 3 _posts_ fields - _id_, _name_ and _totalComments_:

```java
// Create a view collection
ObjectNode viewCollection = pb.collections.createView("post_stats",
    "SELECT posts.id, posts.name, count(comments.id) as totalComments " +
    "FROM posts " +
    "LEFT JOIN comments on comments.postId = posts.id " +
    "GROUP BY posts.id",
    null, null
);
```

**Note**: View collections don't receive realtime events because they don't have create/update/delete operations.

### Auth Collection

**Auth collection** has everything from the **Base collection** but with some additional special fields to help you manage your app users and also provide various authentication options.

Each Auth collection has the following special system fields: `email`, `emailVisibility`, `verified`, `password` and `tokenKey`. They cannot be renamed or deleted but can be configured using their specific field options.

```java
// Create an auth collection
Map<String, Object> usersData = new HashMap<>();
usersData.put("name", "users");
usersData.put("type", "auth");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> nameField = new HashMap<>();
nameField.put("name", "name");
nameField.put("type", "text");
nameField.put("required", true);
fields.add(nameField);

Map<String, Object> roleField = new HashMap<>();
roleField.put("name", "role");
roleField.put("type", "select");
Map<String, Object> roleOptions = new HashMap<>();
roleOptions.put("values", List.of("employee", "staff", "admin"));
roleField.put("options", roleOptions);
fields.add(roleField);

usersData.put("fields", fields);
ObjectNode usersCollection = pb.collections.create(usersData, null, null, null);
```

You can have as many Auth collections as you want (users, managers, staffs, members, clients, etc.) each with their own set of fields, separate login and records managing endpoints.

## Collections API

### Initialize Client

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://localhost:8090");

// Authenticate as superuser (required for collection management)
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
```

### List Collections

```java
// Get paginated list
ResultList result = pb.collections.getList(1, 50, false, null, null, null, null, null, null);

// Get all collections
List<ObjectNode> allCollections = pb.collections.getFullList(200, null, null, null, null, null, null);
```

### Get Collection

```java
// By ID or name
ObjectNode collection = pb.collections.getOne("articles", null, null, null, null);
// or
ObjectNode collection = pb.collections.getOne("COLLECTION_ID", null, null, null, null);
```

### Create Collection

#### Using Scaffolds (Recommended)

```java
// Create base collection
ObjectNode base = pb.collections.createBase("articles", Map.of(
    "fields", List.of(Map.of(
        "name", "title",
        "type", "text",
        "required", true
    ))
), null);

// Create auth collection
ObjectNode auth = pb.collections.createAuth("users", null, null);

// Create view collection
ObjectNode view = pb.collections.createView("stats",
    "SELECT id, name FROM posts",
    null, null
);
```

#### Manual Creation

```java
Map<String, Object> collectionData = new HashMap<>();
collectionData.put("type", "base");
collectionData.put("name", "articles");

List<Map<String, Object>> fields = new ArrayList<>();

Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
titleField.put("min", 6);
titleField.put("max", 100);
fields.add(titleField);

Map<String, Object> descriptionField = new HashMap<>();
descriptionField.put("name", "description");
descriptionField.put("type", "text");
fields.add(descriptionField);

Map<String, Object> publishedField = new HashMap<>();
publishedField.put("name", "published");
publishedField.put("type", "bool");
publishedField.put("required", true);
fields.add(publishedField);

Map<String, Object> viewsField = new HashMap<>();
viewsField.put("name", "views");
viewsField.put("type", "number");
viewsField.put("min", 0);
fields.add(viewsField);

collectionData.put("fields", fields);
collectionData.put("listRule", "@request.auth.id != '' || published = true");
collectionData.put("viewRule", "@request.auth.id != '' || published = true");
collectionData.put("createRule", "@request.auth.id != ''");
collectionData.put("updateRule", "@request.auth.id != ''");
collectionData.put("deleteRule", "@request.auth.id != ''");

ObjectNode collection = pb.collections.create(collectionData, null, null, null);
```

### Update Collection

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != '' || published = true && status = 'public'");

ObjectNode collection = pb.collections.update("articles", updateData, null, null);
```

### Delete Collection

```java
// Warning: This will delete the collection and all its records
pb.collections.deleteCollection("articles", null);
```

### Truncate Collection

Deletes all records but keeps the collection structure:

```java
pb.collections.truncate("articles", null);
```

### Import Collections

```java
List<ObjectNode> collectionsToImport = new ArrayList<>();

Map<String, Object> articlesData = new HashMap<>();
articlesData.put("type", "base");
articlesData.put("name", "articles");
articlesData.put("fields", new ArrayList<>());
// ... add fields

ObjectNode articlesNode = JsonUtils.toJsonNode(articlesData);
collectionsToImport.add((ObjectNode) articlesNode);

// Import collections (deleteMissing will delete collections not in the import list)
pb.collections.importCollections(collectionsToImport, false, null);
```

### Get Scaffolds

```java
ObjectNode scaffolds = pb.collections.getScaffolds(null);
// Returns: { base: {...}, auth: {...}, view: {...} }
```

## Records API

### Get Record Service

```java
// Get a RecordService instance for a collection
RecordService articles = pb.collection("articles");
```

### List Records

```java
// Paginated list 
ResultList result = pb.collection("articles").getList(
    1, 20, false,
    "published = true",
    "-created",
    "author",
    "id,title,description",
    null, null, null, null
);

System.out.println(result.items);      // Array of records
System.out.println(result.page);       // Current page number
System.out.println(result.perPage);    // Items per page
System.out.println(result.totalItems); // Total items count
System.out.println(result.totalPages); // Total pages count

// Get all records (automatically paginates)
List<ObjectNode> allRecords = pb.collection("articles").getFullList(
    200,
    "published = true",
    "-created",
    null, null, null, null
);
```

### Get Single Record

```java
ObjectNode record = pb.collection("articles").getOne(
    "RECORD_ID",
    "author,category",
    "id,title,description,author",
    null, null
);
```

### Get First Matching Record

```java
ObjectNode record = pb.collection("articles").getFirstListItem(
    "title ~ 'example' && published = true",
    "author",
    null, null, null, null, null
);
```

### Create Record

```java
// Simple create
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My First Article");
recordData.put("description", "This is a test article");
recordData.put("published", true);
recordData.put("views", 0);

ObjectNode record = pb.collection("articles").create(recordData, null, null, null);

// With file upload
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Article");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "cover.jpg", "image/jpeg"));
files.put("cover", fileList);

ObjectNode record = pb.collection("articles").create(recordData, files, null, null);
```

### Update Record

```java
// Simple update
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");
updateData.put("published", true);

ObjectNode record = pb.collection("articles").update("RECORD_ID", updateData, null, null, null);

// With file upload
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(newFileBytes, "newcover.jpg", "image/jpeg"));
files.put("cover", fileList);

ObjectNode record = pb.collection("articles").update("RECORD_ID", updateData, files, null, null);
```

### Delete Record

```java
pb.collection("articles").delete("RECORD_ID", null, null);
```

### Batch Operations

```java
BatchService batch = pb.createBatch();
batch.collection("articles").create(Map.of("title", "Article 1"), null, null, null);
batch.collection("articles").create(Map.of("title", "Article 2"), null, null, null);
batch.collection("articles").update("RECORD_ID", Map.of("published", true), null, null, null);

List<JsonNode> batchResult = batch.send();
```

## Field Types

All collection fields (with exception of the `JSONField`) are **non-nullable and use a zero-default** for their respective type as fallback value when missing (empty string for `text`, 0 for `number`, etc.).

### BoolField

Stores a single `false` (default) or `true` value.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "published");
field.put("type", "bool");
field.put("required", true);

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("published", true);
pb.collection("articles").create(recordData, null, null, null);
```

### NumberField

Stores numeric/float64 value: `0` (default), `2`, `-1`, `1.5`.

**Available modifiers:**
- `fieldName+` - adds number to the existing record value
- `fieldName-` - subtracts number from the existing record value

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "views");
field.put("type", "number");
field.put("min", 0);
field.put("max", 1000000);
field.put("onlyInt", false);  // Allow decimals

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("views", 0);
pb.collection("articles").create(recordData, null, null, null);

// Increment
Map<String, Object> updateData = new HashMap<>();
updateData.put("views+", 1);
pb.collection("articles").update("RECORD_ID", updateData, null, null, null);
```

### TextField

Stores string values: `""` (default), `"example"`.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "title");
field.put("type", "text");
field.put("required", true);
field.put("min", 6);
field.put("max", 100);
field.put("pattern", "^[A-Z]");  // Must start with uppercase

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Article");
pb.collection("articles").create(recordData, null, null, null);
```

### EmailField

Stores a single email string address: `""` (default), `"john@example.com"`.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "email");
field.put("type", "email");
field.put("required", true);

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("email", "user@example.com");
pb.collection("users").create(recordData, null, null, null);
```

### URLField

Stores a single URL string value: `""` (default), `"https://example.com"`.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "website");
field.put("type", "url");
field.put("required", false);

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("website", "https://example.com");
pb.collection("users").create(recordData, null, null, null);
```

### EditorField

Stores HTML formatted text: `""` (default), `<p>example</p>`.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "content");
field.put("type", "editor");
field.put("required", true);
field.put("maxSize", 10485760);  // 10MB

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("content", "<p>This is HTML content</p><p>With multiple paragraphs</p>");
pb.collection("articles").create(recordData, null, null, null);
```

### DateField

Stores a single datetime string value: `""` (default), `"2022-01-01 00:00:00.000Z"`.

All BosBase dates follow the RFC3339 format `Y-m-d H:i:s.uZ` (e.g. `2024-11-10 18:45:27.123Z`).

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "published_at");
field.put("type", "date");
field.put("required", false);

// Usage
Map<String, Object> recordData = new HashMap<>();
recordData.put("published_at", "2024-11-10 18:45:27.123Z");
pb.collection("articles").create(recordData, null, null, null);

// Filter by date
ResultList records = pb.collection("articles").getList(
    1, 20, false,
    "created >= '2024-11-19 00:00:00.000Z' && created <= '2024-11-19 23:59:59.999Z'",
    null, null, null, null, null, null
);
```

### SelectField

Stores single or multiple string values from a predefined list.

For **single** `select` (the `MaxSelect` option is <= 1) the field value is a string: `""`, `"optionA"`.

For **multiple** `select` (the `MaxSelect` option is >= 2) the field value is an array: `[]`, `["optionA", "optionB"]`.

**Available modifiers:**
- `fieldName+` - appends one or more values
- `+fieldName` - prepends one or more values
- `fieldName-` - subtracts/removes one or more values

```java
// Single select
Map<String, Object> field = new HashMap<>();
field.put("name", "status");
field.put("type", "select");
Map<String, Object> options = new HashMap<>();
options.put("values", List.of("draft", "published", "archived"));
field.put("options", options);
field.put("maxSelect", 1);

// Multiple select
Map<String, Object> tagsField = new HashMap<>();
tagsField.put("name", "tags");
tagsField.put("type", "select");
Map<String, Object> tagOptions = new HashMap<>();
tagOptions.put("values", List.of("tech", "design", "business", "marketing"));
tagsField.put("options", tagOptions);
tagsField.put("maxSelect", 5);

// Usage - Single
Map<String, Object> recordData = new HashMap<>();
recordData.put("status", "published");
pb.collection("articles").create(recordData, null, null, null);

// Usage - Multiple
Map<String, Object> recordData2 = new HashMap<>();
recordData2.put("tags", List.of("tech", "design"));
pb.collection("articles").create(recordData2, null, null, null);

// Modify - Append
Map<String, Object> updateData = new HashMap<>();
updateData.put("tags+", "marketing");
pb.collection("articles").update("RECORD_ID", updateData, null, null, null);

// Modify - Remove
Map<String, Object> updateData2 = new HashMap<>();
updateData2.put("tags-", "tech");
pb.collection("articles").update("RECORD_ID", updateData2, null, null, null);
```

### FileField

Manages record file(s). BosBase stores in the database only the file name. The file itself is stored either on the local disk or in S3.

For **single** `file` (the `MaxSelect` option is <= 1) the stored value is a string: `""`, `"file1_Ab24ZjL.png"`.

For **multiple** `file` (the `MaxSelect` option is >= 2) the stored value is an array: `[]`, `["file1_Ab24ZjL.png", "file2_Frq24ZjL.txt"]`.

**Available modifiers:**
- `fieldName+` - appends one or more files
- `+fieldName` - prepends one or more files
- `fieldName-` - deletes one or more files

```java
// Single file
Map<String, Object> field = new HashMap<>();
field.put("name", "cover");
field.put("type", "file");
field.put("maxSelect", 1);
field.put("maxSize", 5242880);  // 5MB
field.put("mimeTypes", List.of("image/jpeg", "image/png"));

// Multiple files
Map<String, Object> documentsField = new HashMap<>();
documentsField.put("name", "documents");
documentsField.put("type", "file");
documentsField.put("maxSelect", 10);
documentsField.put("maxSize", 10485760);  // 10MB
documentsField.put("mimeTypes", List.of("application/pdf", "application/docx"));

// Usage - Upload file
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Article");

Map<String, List<FileAttachment>> files = new HashMap<>();
List<FileAttachment> fileList = new ArrayList<>();
// fileList.add(new FileAttachment(fileBytes, "cover.jpg", "image/jpeg"));
files.put("cover", fileList);

ObjectNode record = pb.collection("articles").create(recordData, files, null, null);
```

### RelationField

Stores single or multiple collection record references.

For **single** `relation` (the `MaxSelect` option is <= 1) the field value is a string: `""`, `"RECORD_ID"`.

For **multiple** `relation` (the `MaxSelect` option is >= 2) the field value is an array: `[]`, `["RECORD_ID1", "RECORD_ID2"]`.

**Available modifiers:**
- `fieldName+` - appends one or more ids
- `+fieldName` - prepends one or more ids
- `fieldName-` - subtracts/removes one or more ids

```java
// Single relation
Map<String, Object> field = new HashMap<>();
field.put("name", "author");
field.put("type", "relation");
Map<String, Object> options = new HashMap<>();
options.put("collectionId", "users");
options.put("cascadeDelete", false);
field.put("options", options);
field.put("maxSelect", 1);

// Multiple relation
Map<String, Object> categoriesField = new HashMap<>();
categoriesField.put("name", "categories");
categoriesField.put("type", "relation");
Map<String, Object> catOptions = new HashMap<>();
catOptions.put("collectionId", "categories");
categoriesField.put("options", catOptions);
categoriesField.put("maxSelect", 5);

// Usage - Single
Map<String, Object> recordData = new HashMap<>();
recordData.put("title", "My Article");
recordData.put("author", "USER_RECORD_ID");
pb.collection("articles").create(recordData, null, null, null);

// Usage - Multiple
Map<String, Object> recordData2 = new HashMap<>();
recordData2.put("title", "My Article");
recordData2.put("categories", List.of("CAT_ID1", "CAT_ID2"));
pb.collection("articles").create(recordData2, null, null, null);

// Expand relations when fetching
ObjectNode record = pb.collection("articles").getOne(
    "RECORD_ID",
    "author,categories",
    null, null, null
);
// record.path("expand").path("author") - full author record
// record.path("expand").path("categories") - array of category records
```

### JSONField

Stores any serialized JSON value, including `null` (default). This is the only nullable field type.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "metadata");
field.put("type", "json");
field.put("required", false);

// Usage
Map<String, Object> recordData = new HashMap<>();
Map<String, Object> metadata = new HashMap<>();
Map<String, Object> seo = new HashMap<>();
seo.put("title", "SEO Title");
seo.put("description", "SEO Description");
metadata.put("seo", seo);

Map<String, Object> custom = new HashMap<>();
custom.put("tags", List.of("tag1", "tag2"));
custom.put("priority", 10);
metadata.put("custom", custom);

recordData.put("title", "My Article");
recordData.put("metadata", metadata);
pb.collection("articles").create(recordData, null, null, null);
```

### GeoPointField

Stores geographic coordinates (longitude, latitude) as a serialized json object.

The default/zero value of a `geoPoint` is the "Null Island", aka. `{"lon":0,"lat":0}`.

```java
// Create field
Map<String, Object> field = new HashMap<>();
field.put("name", "location");
field.put("type", "geoPoint");
field.put("required", false);

// Usage
Map<String, Object> recordData = new HashMap<>();
Map<String, Double> location = new HashMap<>();
location.put("lon", 139.6917);
location.put("lat", 35.6586);
recordData.put("name", "Tokyo Tower");
recordData.put("location", location);
pb.collection("places").create(recordData, null, null, null);
```

## Examples

### Complete Example: Blog System

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

public class BlogExample {
    public static void main(String[] args) {
        BosBase pb = new BosBase("http://localhost:8090");
        pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
        
        // 1. Create users (auth) collection
        Map<String, Object> usersData = new HashMap<>();
        usersData.put("name", "users");
        usersData.put("type", "auth");
        
        List<Map<String, Object>> userFields = new ArrayList<>();
        Map<String, Object> nameField = new HashMap<>();
        nameField.put("name", "name");
        nameField.put("type", "text");
        nameField.put("required", true);
        userFields.add(nameField);
        
        Map<String, Object> avatarField = new HashMap<>();
        avatarField.put("name", "avatar");
        avatarField.put("type", "file");
        avatarField.put("maxSelect", 1);
        avatarField.put("mimeTypes", List.of("image/jpeg", "image/png"));
        userFields.add(avatarField);
        
        usersData.put("fields", userFields);
        ObjectNode usersCollection = pb.collections.create(usersData, null, null, null);
        
        // 2. Create categories (base) collection
        Map<String, Object> categoriesData = new HashMap<>();
        categoriesData.put("name", "categories");
        categoriesData.put("type", "base");
        
        List<Map<String, Object>> categoryFields = new ArrayList<>();
        Map<String, Object> catNameField = new HashMap<>();
        catNameField.put("name", "name");
        catNameField.put("type", "text");
        catNameField.put("required", true);
        categoryFields.add(catNameField);
        
        Map<String, Object> slugField = new HashMap<>();
        slugField.put("name", "slug");
        slugField.put("type", "text");
        slugField.put("required", true);
        categoryFields.add(slugField);
        
        categoriesData.put("fields", categoryFields);
        ObjectNode categoriesCollection = pb.collections.create(categoriesData, null, null, null);
        
        // 3. Create articles (base) collection
        // ... (similar structure)
        
        System.out.println("Blog setup complete!");
    }
}
```

### Realtime Subscriptions

```java
// Subscribe to all changes in a collection
Runnable unsubscribeAll = pb.collection("articles").subscribe("*", (e) -> {
    System.out.println("Action: " + e.get("action")); // 'create', 'update', or 'delete'
    System.out.println("Record: " + e.get("record"));
}, null, null);

// Subscribe to changes in a specific record
Runnable unsubscribeRecord = pb.collection("articles").subscribe("RECORD_ID", (e) -> {
    System.out.println("Record updated: " + e.get("record"));
}, null, null);

// Unsubscribe
pb.collection("articles").unsubscribe("RECORD_ID");
pb.collection("articles").unsubscribe("*");
pb.collection("articles").unsubscribe(null); // Unsubscribe from all
```

### Authentication with Auth Collections

```java
// Create an auth collection
Map<String, Object> customersData = new HashMap<>();
customersData.put("name", "customers");
customersData.put("type", "auth");
// ... add fields

ObjectNode customersCollection = pb.collections.create(customersData, null, null, null);

// Register a new customer
Map<String, Object> customerData = new HashMap<>();
customerData.put("email", "customer@example.com");
customerData.put("emailVisibility", true);
customerData.put("password", "password123");
customerData.put("passwordConfirm", "password123");
customerData.put("name", "Jane Doe");
customerData.put("phone", "+1234567890");

ObjectNode customer = pb.collection("customers").create(customerData, null, null, null);

// Authenticate
ObjectNode auth = pb.collection("customers").authWithPassword(
    "customer@example.com",
    "password123",
    null, null, null, null, null, null, null, null
);

System.out.println(auth.path("token").asText()); // Auth token
System.out.println(auth.path("record")); // Customer record

// Check if authenticated
if (pb.authStore.isValid()) {
    System.out.println("Current user: " + pb.authStore.getModel());
}

// Logout
pb.authStore.clear();
```

