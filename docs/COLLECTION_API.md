# Collection API - Java SDK Documentation

## Overview

The Collection API provides endpoints for managing collections (Base, Auth, and View types). All operations require superuser authentication and allow you to create, read, update, and delete collections along with their schemas and configurations.

**Key Features:**
- List and search collections
- View collection details
- Create collections (base, auth, view)
- Update collection schemas and rules
- Delete collections
- Truncate collections (delete all records)
- Import collections in bulk
- Get collection scaffolds (templates)

**Backend Endpoints:**
- `GET /api/collections` - List collections
- `GET /api/collections/{collection}` - View collection
- `POST /api/collections` - Create collection
- `PATCH /api/collections/{collection}` - Update collection
- `DELETE /api/collections/{collection}` - Delete collection
- `DELETE /api/collections/{collection}/truncate` - Truncate collection
- `PUT /api/collections/import` - Import collections
- `GET /api/collections/meta/scaffolds` - Get scaffolds

**Note**: All Collection API operations require superuser authentication.

## Authentication

All Collection API operations require superuser authentication:

```java
import com.bosbase.sdk.BosBase;

BosBase pb = new BosBase("http://127.0.0.1:8090");

// Authenticate as superuser
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
// OR
pb.collection("_superusers").authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
```

## List Collections

Returns a paginated list of collections with support for filtering and sorting.

```java
// Basic list
ResultList result = pb.collections.getList(1, 30, false, null, null, null, null, null, null);

System.out.println(result.page);        // 1
System.out.println(result.perPage);     // 30
System.out.println(result.totalItems);  // Total collections count
System.out.println(result.items);       // Array of collections
```

### Advanced Filtering and Sorting

```java
// Filter by type
ResultList authCollections = pb.collections.getList(
    1, 100, false,
    "type = 'auth'",
    null, null, null, null, null, null
);

// Filter by name pattern
ResultList matchingCollections = pb.collections.getList(
    1, 100, false,
    "name ~ 'user'",
    null, null, null, null, null, null
);

// Sort by creation date
ResultList sortedCollections = pb.collections.getList(
    1, 100, false,
    null,
    "-created",
    null, null, null, null, null
);

// Complex filter
ResultList filtered = pb.collections.getList(
    1, 100, false,
    "type = 'base' && system = false && created >= '2023-01-01'",
    "name",
    null, null, null, null, null
);
```

### Get Full List

```java
// Get all collections at once
List<ObjectNode> allCollections = pb.collections.getFullList(
    200,
    "system = false",
    "name",
    null, null, null, null
);
```

### Get First Matching Collection

```java
// Get first auth collection
ObjectNode authCollection = pb.collections.getFirstListItem(
    "type = 'auth'",
    null, null, null, null, null, null
);
```

## View Collection

Retrieve a single collection by ID or name:

```java
// By name
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);

// By ID
ObjectNode collection = pb.collections.getOne("_pbc_2287844090", null, null, null, null);

// With field selection
ObjectNode collection = pb.collections.getOne(
    "posts",
    null,
    "id,name,type,fields.name,fields.type",
    null, null
);
```

## Create Collection

Create a new collection with schema fields and configuration.

**Note**: If the `created` and `updated` fields are not specified during collection initialization, BosBase will automatically create them. These system fields are added to all collections by default and track when records are created and last modified. You don't need to include them in your field definitions.

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
titleField.put("min", 10);
titleField.put("max", 255);
fields.add(titleField);

Map<String, Object> contentField = new HashMap<>();
contentField.put("name", "content");
contentField.put("type", "editor");
contentField.put("required", false);
fields.add(contentField);

Map<String, Object> publishedField = new HashMap<>();
publishedField.put("name", "published");
publishedField.put("type", "bool");
publishedField.put("required", false);
fields.add(publishedField);

Map<String, Object> authorField = new HashMap<>();
authorField.put("name", "author");
authorField.put("type", "relation");
authorField.put("required", true);
authorField.put("collectionId", "_pbc_users_auth_");
authorField.put("maxSelect", 1);
fields.add(authorField);

collectionData.put("fields", fields);
collectionData.put("listRule", "@request.auth.id != ''");
collectionData.put("viewRule", "@request.auth.id != '' || published = true");
collectionData.put("createRule", "@request.auth.id != ''");
collectionData.put("updateRule", "author = @request.auth.id");
collectionData.put("deleteRule", "author = @request.auth.id");

ObjectNode baseCollection = pb.collections.create(collectionData, null, null, null);
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

Map<String, Object> avatarField = new HashMap<>();
avatarField.put("name", "avatar");
avatarField.put("type", "file");
avatarField.put("required", false);
avatarField.put("maxSelect", 1);
avatarField.put("maxSize", 2097152); // 2MB
avatarField.put("mimeTypes", List.of("image/jpeg", "image/png"));
fields.add(avatarField);

authCollectionData.put("fields", fields);
authCollectionData.put("listRule", null);
authCollectionData.put("viewRule", "@request.auth.id = id");
authCollectionData.put("createRule", null);
authCollectionData.put("updateRule", "@request.auth.id = id");
authCollectionData.put("deleteRule", "@request.auth.id = id");
authCollectionData.put("manageRule", null);
authCollectionData.put("authRule", "verified = true"); // Only verified users can authenticate

Map<String, Object> passwordAuth = new HashMap<>();
passwordAuth.put("enabled", true);
passwordAuth.put("identityFields", List.of("email", "username"));
authCollectionData.put("passwordAuth", passwordAuth);

Map<String, Object> authToken = new HashMap<>();
authToken.put("duration", 604800); // 7 days
authCollectionData.put("authToken", authToken);

ObjectNode authCollection = pb.collections.create(authCollectionData, null, null, null);
```

### Create View Collection

```java
Map<String, Object> viewData = new HashMap<>();
viewData.put("name", "published_posts");
viewData.put("type", "view");
viewData.put("listRule", "@request.auth.id != ''");
viewData.put("viewRule", "@request.auth.id != ''");
viewData.put("viewQuery", 
    "SELECT p.id, p.title, p.content, p.created, " +
    "u.name as author_name, u.email as author_email " +
    "FROM posts p " +
    "LEFT JOIN users u ON p.author = u.id " +
    "WHERE p.published = true"
);

ObjectNode viewCollection = pb.collections.create(viewData, null, null, null);
```

### Create from Scaffold

Use predefined scaffolds as a starting point:

```java
// Get available scaffolds
ObjectNode scaffolds = pb.collections.getScaffolds(null);

// Create base collection from scaffold
ObjectNode baseCollection = pb.collections.createBase("my_posts", Map.of(
    "fields", List.of(Map.of(
        "name", "title",
        "type", "text",
        "required", true
    ))
), null);

// Create auth collection from scaffold
ObjectNode authCollection = pb.collections.createAuth("my_users", Map.of(
    "passwordAuth", Map.of(
        "enabled", true,
        "identityFields", List.of("email")
    )
), null);

// Create view collection from scaffold
ObjectNode viewCollection = pb.collections.createView(
    "my_view",
    "SELECT id, title FROM posts",
    Map.of("listRule", "@request.auth.id != ''"),
    null
);
```

### Accessing Collection ID After Creation

When a collection is successfully created, the returned `ObjectNode` includes the `id` property, which contains the unique identifier assigned by the backend. You can access it immediately after creation:

```java
// Create a collection and access its ID
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

// Access the collection ID
System.out.println(collection.path("id").asText()); // e.g., "_pbc_2287844090"

// Use the ID for subsequent operations
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != ''");
pb.collections.update(collection.path("id").asText(), updateData, null, null);

// Store the ID for later use
String collectionId = collection.path("id").asText();
```

## Update Collection

Update an existing collection's schema, fields, or rules:

```java
// Update collection name and rules
Map<String, Object> updateData = new HashMap<>();
updateData.put("name", "articles");
updateData.put("listRule", "@request.auth.id != '' || status = 'public'");
updateData.put("viewRule", "@request.auth.id != '' || status = 'public'");

ObjectNode updated = pb.collections.update("posts", updateData, null, null);

// Add new field
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
ArrayNode fields = (ArrayNode) collection.path("fields");

Map<String, Object> newField = new HashMap<>();
newField.put("name", "tags");
newField.put("type", "select");
Map<String, Object> options = new HashMap<>();
options.put("values", List.of("tech", "science", "art"));
newField.put("options", options);

pb.collections.addField("posts", newField, null);

// Update field configuration
ObjectNode collection = pb.collections.getOne("posts", null, null, null, null);
// Modify field in collection object
pb.collections.update("posts", JsonUtils.jsonNodeToMap(collection), null, null);
```

## Manage Indexes

BosBase stores collection indexes as SQL expressions on the `indexes` property of a collection. The Java SDK provides dedicated helpers so you don't have to manually craft the SQL or resend the full collection payload every time you want to adjust an index.

### Add or Update Indexes

```java
// Create a unique slug index (index names are optional)
pb.collections.addIndex("posts", List.of("slug"), true, "idx_posts_slug_unique", null);

// Composite (non-unique) index; defaults to idx_{collection}_{columns}
pb.collections.addIndex("posts", List.of("status", "published"), false, null, null);
```

- `collectionIdOrName` can be either the collection name or internal id.
- `columns` must reference existing columns (system fields such as `id`, `created`, and `updated` are allowed).
- `unique` (default `false`) controls whether `CREATE UNIQUE INDEX` or `CREATE INDEX` is generated.
- `indexName` is optional; omit it to let the SDK generate `idx_{collection}_{column1}_{column2}` automatically.

Calling `addIndex` twice with the same name replaces the definition on the backend, making it easy to iterate on your schema.

### Remove Indexes

```java
// Remove the index that targets the slug column
pb.collections.removeIndex("posts", List.of("slug"), null);
```

`removeIndex` looks for indexes that contain all of the provided columns (in any order) and drops them from the collection. This deletes the actual database index when the collection is saved.

### List Indexes

```java
List<String> indexes = pb.collections.getIndexes("posts", null);

for (String idx : indexes) {
    System.out.println(idx);
}
// => CREATE UNIQUE INDEX `idx_posts_slug_unique` ON `posts` (`slug`)
```

`getIndexes` returns the raw SQL strings stored on the collection so you can audit existing indexes or decide whether you need to create new ones.

## Delete Collection

Delete a collection (including all records and files):

```java
// Delete by name
pb.collections.deleteCollection("old_collection", null);

// Delete by ID
pb.collections.deleteCollection("_pbc_2287844090", null);
```

**Warning**: This operation is destructive and will:
- Delete the collection schema
- Delete all records in the collection
- Delete all associated files
- Remove all indexes

**Note**: Collections referenced by other collections cannot be deleted.

## Truncate Collection

Delete all records in a collection while keeping the collection schema:

```java
// Truncate collection (delete all records)
pb.collections.truncate("posts", null);

// This will:
// - Delete all records in the collection
// - Delete all associated files
// - Delete cascade-enabled relations
// - Keep the collection schema intact
```

**Warning**: This operation is destructive and cannot be undone. It's useful for:
- Clearing test data
- Resetting collections
- Bulk data removal

**Note**: View collections cannot be truncated.

## Import Collections

Bulk import multiple collections at once:

```java
List<ObjectNode> collectionsToImport = new ArrayList<>();

Map<String, Object> postsCollection = new HashMap<>();
postsCollection.put("name", "posts");
postsCollection.put("type", "base");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
fields.add(titleField);

Map<String, Object> contentField = new HashMap<>();
contentField.put("name", "content");
contentField.put("type", "editor");
fields.add(contentField);

postsCollection.put("fields", fields);
postsCollection.put("listRule", "@request.auth.id != ''");

ObjectNode postsNode = JsonUtils.toJsonNode(postsCollection);
collectionsToImport.add((ObjectNode) postsNode);

// Import without deleting existing collections
pb.collections.importCollections(collectionsToImport, false, null);

// Import and delete collections not in the import list
pb.collections.importCollections(collectionsToImport, true, null);
```

### Import Options

- **`deleteMissing: false`** (default): Only create/update collections in the import list
- **`deleteMissing: true`**: Delete all collections not present in the import list

**Warning**: Using `deleteMissing: true` will permanently delete collections and all their data.

## Get Scaffolds

Get collection templates for creating new collections:

```java
ObjectNode scaffolds = pb.collections.getScaffolds(null);

// Available scaffold types
ObjectNode baseScaffold = (ObjectNode) scaffolds.path("base");
ObjectNode authScaffold = (ObjectNode) scaffolds.path("auth");
ObjectNode viewScaffold = (ObjectNode) scaffolds.path("view");

// Use scaffold as starting point
Map<String, Object> baseTemplate = JsonUtils.jsonNodeToMap(baseScaffold);
Map<String, Object> newCollection = new HashMap<>(baseTemplate);
newCollection.put("name", "my_collection");

List<Map<String, Object>> fields = new ArrayList<>();
// Add custom fields
fields.add(Map.of("name", "custom_field", "type", "text"));
newCollection.put("fields", fields);

ObjectNode collection = pb.collections.create(newCollection, null, null, null);
```

## Complete Examples

### Example 1: Setup Blog Collections

```java
public Map<String, String> setupBlog() {
    // Create posts collection
    Map<String, Object> postsData = new HashMap<>();
    postsData.put("name", "posts");
    postsData.put("type", "base");
    
    List<Map<String, Object>> fields = new ArrayList<>();
    
    Map<String, Object> titleField = new HashMap<>();
    titleField.put("name", "title");
    titleField.put("type", "text");
    titleField.put("required", true);
    titleField.put("min", 10);
    titleField.put("max", 255);
    fields.add(titleField);
    
    // ... add more fields
    
    postsData.put("fields", fields);
    postsData.put("listRule", "@request.auth.id != '' || published = true");
    postsData.put("viewRule", "@request.auth.id != '' || published = true");
    postsData.put("createRule", "@request.auth.id != ''");
    postsData.put("updateRule", "author = @request.auth.id");
    postsData.put("deleteRule", "author = @request.auth.id");
    
    ObjectNode posts = pb.collections.create(postsData, null, null, null);
    
    // Create categories collection
    Map<String, Object> categoriesData = new HashMap<>();
    categoriesData.put("name", "categories");
    categoriesData.put("type", "base");
    
    List<Map<String, Object>> categoryFields = new ArrayList<>();
    Map<String, Object> nameField = new HashMap<>();
    nameField.put("name", "name");
    nameField.put("type", "text");
    nameField.put("required", true);
    nameField.put("unique", true);
    categoryFields.add(nameField);
    
    categoriesData.put("fields", categoryFields);
    categoriesData.put("listRule", "@request.auth.id != ''");
    categoriesData.put("viewRule", "@request.auth.id != ''");
    
    ObjectNode categories = pb.collections.create(categoriesData, null, null, null);
    
    // Access collection IDs immediately after creation
    System.out.println("Posts collection ID: " + posts.path("id").asText());
    System.out.println("Categories collection ID: " + categories.path("id").asText());
    
    Map<String, String> result = new HashMap<>();
    result.put("postsId", posts.path("id").asText());
    result.put("categoriesId", categories.path("id").asText());
    return result;
}
```

## Error Handling

```java
try {
    Map<String, Object> collectionData = new HashMap<>();
    collectionData.put("name", "test");
    collectionData.put("type", "base");
    collectionData.put("fields", new ArrayList<>());
    
    ObjectNode collection = pb.collections.create(collectionData, null, null, null);
} catch (ClientResponseError error) {
    if (error.status == 401) {
        System.err.println("Not authenticated");
    } else if (error.status == 403) {
        System.err.println("Not a superuser");
    } else if (error.status == 400) {
        System.err.println("Validation error: " + error.response);
    } else {
        System.err.println("Unexpected error: " + error);
    }
}
```

## Best Practices

1. **Always Authenticate**: Ensure you're authenticated as a superuser before making requests
2. **Backup Before Import**: Always backup existing collections before using `import` with `deleteMissing: true`
3. **Validate Schema**: Validate collection schemas before creating/updating
4. **Use Scaffolds**: Use scaffolds as starting points for consistency
5. **Test Rules**: Test API rules thoroughly before deploying to production
6. **Index Important Fields**: Add indexes for frequently queried fields
7. **Document Schemas**: Keep documentation of your collection schemas
8. **Version Control**: Store collection schemas in version control for migration tracking

## Limitations

- **Superuser Only**: All operations require superuser authentication
- **System Collections**: System collections cannot be deleted or renamed
- **View Collections**: Cannot be truncated (they don't store records)
- **Relations**: Collections referenced by others cannot be deleted
- **Field Modifications**: Some field type changes may require data migration

## Related Documentation

- [Collections Guide](./COLLECTIONS.md) - Working with collections and records
- [API Records](./API_RECORDS.md) - Record CRUD operations
- [API Rules and Filters](./API_RULES_AND_FILTERS.md) - Understanding API rules

