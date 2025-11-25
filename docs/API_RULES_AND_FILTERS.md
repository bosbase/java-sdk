# API Rules and Filters - Java SDK Documentation

## Overview

API Rules are your collection access controls and data filters. They control who can perform actions on your collections and what data they can access.

Each collection has 5 rules, corresponding to specific API actions:
- `listRule` - Controls who can list records
- `viewRule` - Controls who can view individual records
- `createRule` - Controls who can create records
- `updateRule` - Controls who can update records
- `deleteRule` - Controls who can delete records

Auth collections have an additional `manageRule` that allows one user to fully manage another user's data.

## Rule Values

Each rule can be set to:

- **`null` (locked)** - Only authorized superusers can perform the action (default)
- **Empty string `""`** - Anyone can perform the action (superusers, authenticated users, and guests)
- **Non-empty string** - Only users that satisfy the filter expression can perform the action

## Important Notes

1. **Rules act as filters**: API Rules also act as record filters. For example, setting `listRule` to `status = "active"` will only return active records.
2. **HTTP Status Codes**: 
   - `200` with empty items for unsatisfied `listRule`
   - `400` for unsatisfied `createRule`
   - `404` for unsatisfied `viewRule`, `updateRule`, `deleteRule`
   - `403` for locked rules when not a superuser
3. **Superuser bypass**: API Rules are ignored when the action is performed by an authorized superuser.

## Setting Rules via SDK

### Java SDK

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Create collection with rules
Map<String, Object> collectionData = new HashMap<>();
collectionData.put("name", "articles");
collectionData.put("type", "base");

List<Map<String, Object>> fields = new ArrayList<>();
Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
fields.add(titleField);

Map<String, Object> statusField = new HashMap<>();
statusField.put("name", "status");
statusField.put("type", "select");
Map<String, Object> statusOptions = new HashMap<>();
statusOptions.put("values", List.of("draft", "published"));
statusField.put("options", statusOptions);
statusField.put("maxSelect", 1);
fields.add(statusField);

Map<String, Object> authorField = new HashMap<>();
authorField.put("name", "author");
authorField.put("type", "relation");
authorField.put("collectionId", "users");
authorField.put("maxSelect", 1);
fields.add(authorField);

collectionData.put("fields", fields);
collectionData.put("listRule", "@request.auth.id != '' || status = 'published'");
collectionData.put("viewRule", "@request.auth.id != '' || status = 'published'");
collectionData.put("createRule", "@request.auth.id != ''");
collectionData.put("updateRule", "author = @request.auth.id || @request.auth.role = 'admin'");
collectionData.put("deleteRule", "author = @request.auth.id || @request.auth.role = 'admin'");

ObjectNode collection = pb.collections.create(collectionData, null, null, null);

// Update rules
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != '' && (status = 'published' || status = 'draft')");
pb.collections.update("articles", updateData, null, null);

// Remove rule (set to empty string for public access)
Map<String, Object> publicAccess = new HashMap<>();
publicAccess.put("listRule", "");  // Anyone can list
pb.collections.update("articles", publicAccess, null, null);

// Lock rule (set to null for superuser only)
Map<String, Object> lockedRule = new HashMap<>();
lockedRule.put("deleteRule", null);  // Only superusers can delete
pb.collections.update("articles", lockedRule, null, null);
```

## Filter Syntax

The syntax follows: `OPERAND OPERATOR OPERAND`

### Operators

**Comparison Operators:**
- `=` - Equal
- `!=` - NOT equal
- `>` - Greater than
- `>=` - Greater than or equal
- `<` - Less than
- `<=` - Less than or equal

**String Operators:**
- `~` - Like/Contains (auto-wraps right operand in `%` for wildcard match)
- `!~` - NOT Like/Contains

**Array Operators (Any/At least one of):**
- `?=` - Any Equal
- `?!=` - Any NOT equal
- `?>` - Any Greater than
- `?>=` - Any Greater than or equal
- `?<` - Any Less than
- `?<=` - Any Less than or equal
- `?~` - Any Like/Contains
- `?!~` - Any NOT Like/Contains

**Logical Operators:**
- `&&` - AND
- `||` - OR
- `()` - Grouping
- `//` - Single line comments

## Special Identifiers

### @request.*

Access current request data:

**@request.context** - The context where the rule is used
```java
String listRule = "@request.context != 'oauth2'";
```

**@request.method** - HTTP request method
```java
String updateRule = "@request.method = 'PATCH'";
```

**@request.headers.*** - Request headers (normalized to lowercase, `-` replaced with `_`)
```java
String listRule = "@request.headers.x_token = 'test'";
```

**@request.query.*** - Query parameters
```java
String listRule = "@request.query.page = '1'";
```

**@request.auth.*** - Current authenticated user
```java
String listRule = "@request.auth.id != ''";
String viewRule = "@request.auth.email = 'admin@example.com'";
String updateRule = "@request.auth.role = 'admin'";
```

**@request.body.*** - Submitted body parameters
```java
String createRule = "@request.body.title != ''";
String updateRule = "@request.body.status:isset = false";  // Prevent changing status
```

### @collection.*

Target other collections that aren't directly related:

```java
// Check if user has access to related collection
String listRule = "@request.auth.id != '' && @collection.news.categoryId ?= categoryId && @collection.news.author ?= @request.auth.id";
```

### @ Macros (Datetime)

All macros are UTC-based:

- `@now` - Current datetime as string
- `@second` - Current second (0-59)
- `@minute` - Current minute (0-59)
- `@hour` - Current hour (0-23)
- `@weekday` - Current weekday (0-6)
- `@day` - Current day
- `@month` - Current month
- `@year` - Current year
- `@yesterday` - Yesterday datetime
- `@tomorrow` - Tomorrow datetime
- `@todayStart` - Beginning of current day
- `@todayEnd` - End of current day
- `@monthStart` - Beginning of current month
- `@monthEnd` - End of current month
- `@yearStart` - Beginning of current year
- `@yearEnd` - End of current year

**Example:**
```java
String listRule = "@request.body.publicDate >= @now";
String listRule = "created >= @todayStart && created <= @todayEnd";
```

## Field Modifiers

### :isset

Check if a field was submitted in the request (only for `@request.*` fields):

```java
// Prevent changing role field
String updateRule = "@request.body.role:isset = false";

// Require email field
String createRule = "@request.body.email:isset = true";
```

### :length

Check the number of items in an array field (multiple file, select, relation):

```java
// Check submitted array length
String createRule = "@request.body.tags:length > 1 && @request.body.tags:length <= 5";

// Check existing record array length
String listRule = "categories:length = 2";
String listRule = "documents:length >= 1";
```

### :each

Apply condition on each item in an array field:

```java
// Check if all submitted select options contain "create"
String createRule = "@request.body.permissions:each ~ 'create'";

// Check if all existing field values have "pb_" prefix
String listRule = "tags:each ~ 'pb_%'";
```

### :lower

Perform case-insensitive string comparisons:

```java
// Case-insensitive comparison
String listRule = "@request.body.title:lower = 'test'";
String updateRule = "status:lower ~ 'active'";
```

## geoDistance Function

Calculate Haversine distance between two geographic points in kilometers:

```java
// Offices within 25km of location
String listRule = "geoDistance(address.lon, address.lat, 23.32, 42.69) < 25";

// Using request data
String listRule = "geoDistance(location.lon, location.lat, @request.query.lon, @request.query.lat) < @request.query.radius";
```

## Common Rule Examples

### Allow Only Authenticated Users

```java
String listRule = "@request.auth.id != ''";
String viewRule = "@request.auth.id != ''";
String createRule = "@request.auth.id != ''";
String updateRule = "@request.auth.id != ''";
String deleteRule = "@request.auth.id != ''";
```

### Owner-Based Access

```java
String viewRule = "@request.auth.id != '' && author = @request.auth.id";
String updateRule = "@request.auth.id != '' && author = @request.auth.id";
String deleteRule = "@request.auth.id != '' && author = @request.auth.id";
```

### Role-Based Access

```java
// Assuming users have a "role" select field
String listRule = "@request.auth.id != '' && @request.auth.role = 'admin'";
String updateRule = "@request.auth.role = 'admin' || author = @request.auth.id";
```

### Public with Authentication

```java
// Public can view published, authenticated can view all
String listRule = "@request.auth.id != '' || status = 'published'";
String viewRule = "@request.auth.id != '' || status = 'published'";
```

### Filtered Results

```java
// Only show active records
String listRule = "status = 'active'";

// Only show records from last 30 days
String listRule = "created >= @yesterday";

// Only show records matching user's organization
String listRule = "@request.auth.id != '' && organization = @request.auth.organization";
```

### Complex Rules

```java
// Multiple conditions
String listRule = "@request.auth.id != '' && (status = 'published' || status = 'draft') && author = @request.auth.id";

// Nested relations
String listRule = "@request.auth.id != '' && author.role = 'staff'";

// Back relations
String listRule = "@request.auth.id != '' && comments_via_author.id != ''";
```

## Using Filters in Queries

Filters can also be used in regular queries (not just rules):

```java
// List with filter
ResultList result = pb.collection("articles").getList(
    1, 20, false,
    "status = 'published' && created >= @todayStart",
    null, null, null, null, null, null
);

// Complex filter
ResultList result = pb.collection("articles").getList(
    1, 20, false,
    "(title ~ 'test' || description ~ 'test') && status = 'published'",
    null, null, null, null, null, null
);

// Using relation filters
ResultList result = pb.collection("articles").getList(
    1, 20, false,
    "author.role = 'admin' && categories.id ?= 'CAT_ID'",
    null, null, null, null, null, null
);

// Geo distance filter
ResultList result = pb.collection("offices").getList(
    1, 20, false,
    "geoDistance(location.lon, location.lat, 23.32, 42.69) < 25",
    null, null, null, null, null, null
);
```

## Complete Example

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

// Create users collection with role field
Map<String, Object> usersData = new HashMap<>();
usersData.put("name", "users");
usersData.put("type", "auth");

List<Map<String, Object>> userFields = new ArrayList<>();
Map<String, Object> nameField = new HashMap<>();
nameField.put("name", "name");
nameField.put("type", "text");
nameField.put("required", true);
userFields.add(nameField);

Map<String, Object> roleField = new HashMap<>();
roleField.put("name", "role");
roleField.put("type", "select");
Map<String, Object> roleOptions = new HashMap<>();
roleOptions.put("values", List.of("user", "staff", "admin"));
roleField.put("options", roleOptions);
roleField.put("maxSelect", 1);
userFields.add(roleField);

usersData.put("fields", userFields);
ObjectNode users = pb.collections.create(usersData, null, null, null);

// Create articles collection with comprehensive rules
Map<String, Object> articlesData = new HashMap<>();
articlesData.put("name", "articles");
articlesData.put("type", "base");

List<Map<String, Object>> articleFields = new ArrayList<>();
Map<String, Object> titleField = new HashMap<>();
titleField.put("name", "title");
titleField.put("type", "text");
titleField.put("required", true);
articleFields.add(titleField);

Map<String, Object> contentField = new HashMap<>();
contentField.put("name", "content");
contentField.put("type", "editor");
contentField.put("required", true);
articleFields.add(contentField);

Map<String, Object> statusField = new HashMap<>();
statusField.put("name", "status");
statusField.put("type", "select");
Map<String, Object> statusOptions = new HashMap<>();
statusOptions.put("values", List.of("draft", "published", "archived"));
statusField.put("options", statusOptions);
statusField.put("maxSelect", 1);
articleFields.add(statusField);

Map<String, Object> authorField = new HashMap<>();
authorField.put("name", "author");
authorField.put("type", "relation");
authorField.put("collectionId", users.path("id").asText());
authorField.put("maxSelect", 1);
authorField.put("required", true);
articleFields.add(authorField);

articlesData.put("fields", articleFields);
// Public can see published, authenticated can see their own or published
articlesData.put("listRule", "@request.auth.id != '' && (author = @request.auth.id || status = 'published') || status = 'published'");
articlesData.put("viewRule", "@request.auth.id != '' && (author = @request.auth.id || status = 'published') || status = 'published'");
articlesData.put("createRule", "@request.auth.id != ''");
articlesData.put("updateRule", "@request.auth.id != '' && (author = @request.auth.id || @request.auth.role = 'admin') && (@request.body.status:isset = false || status != 'published')");
articlesData.put("deleteRule", "@request.auth.id != '' && (author = @request.auth.id || @request.auth.role = 'admin')");

ObjectNode articles = pb.collections.create(articlesData, null, null, null);

// Authenticate as regular user
pb.collection("users").authWithPassword("user@example.com", "password123", null, null, null, null, null, null, null, null);

// User can create article
Map<String, Object> articleData = new HashMap<>();
articleData.put("title", "My Article");
articleData.put("content", "<p>Content</p>");
articleData.put("status", "draft");
articleData.put("author", pb.authStore.getModel().path("id").asText());

ObjectNode article = pb.collection("articles").create(articleData, null, null, null);

// User can update their own article
Map<String, Object> updateData = new HashMap<>();
updateData.put("title", "Updated Title");
pb.collection("articles").update(article.path("id").asText(), updateData, null, null, null);

// User can list their own articles or published ones
ResultList myArticles = pb.collection("articles").getList(
    1, 20, false,
    "author = @request.auth.id",
    null, null, null, null, null, null
);

// User can also query with additional filters
ResultList published = pb.collection("articles").getList(
    1, 20, false,
    "status = 'published' && created >= @todayStart",
    null, null, null, null, null, null
);
```

