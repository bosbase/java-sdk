# API Rules Documentation - Java SDK

API Rules are collection access controls and data filters that determine who can perform actions on your collections and what data they can access.

## Overview

Each collection has 5 standard API rules, corresponding to specific API actions:

- **`listRule`** - Controls read/list access
- **`viewRule`** - Controls read/view access  
- **`createRule`** - Controls create access
- **`updateRule`** - Controls update access
- **`deleteRule`** - Controls delete access

Auth collections have two additional rules:

- **`manageRule`** - Admin-like permissions for managing auth records
- **`authRule`** - Additional constraints applied during authentication

## Rule Values

Each rule can be set to one of three values:

### 1. `null` (Locked)
Only authorized superusers can perform the action.

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", null);
pb.collections.update("products", updateData, null, null);
```

### 2. `""` (Empty String - Public)
Anyone (superusers, authorized users, and guests) can perform the action.

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "");
pb.collections.update("products", updateData, null, null);
```

### 3. Non-empty String (Filter Expression)
Only users satisfying the filter expression can perform the action.

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != \"\"");
pb.collections.update("products", updateData, null, null);
```

## Setting Rules

### Individual Rules

Set individual rules by updating the collection:

```java
// Set list rule
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != \"\"");
pb.collections.update("products", updateData, null, null);

// Set view rule
Map<String, Object> viewRuleData = new HashMap<>();
viewRuleData.put("viewRule", "@request.auth.id != \"\"");
pb.collections.update("products", viewRuleData, null, null);

// Set create rule
Map<String, Object> createRuleData = new HashMap<>();
createRuleData.put("createRule", "@request.auth.id != \"\"");
pb.collections.update("products", createRuleData, null, null);

// Set update rule
Map<String, Object> updateRuleData = new HashMap<>();
updateRuleData.put("updateRule", "@request.auth.id != \"\" && author.id ?= @request.auth.id");
pb.collections.update("products", updateRuleData, null, null);

// Set delete rule
Map<String, Object> deleteRuleData = new HashMap<>();
deleteRuleData.put("deleteRule", null);  // Only superusers
pb.collections.update("products", deleteRuleData, null, null);
```

### Bulk Rule Updates

Set multiple rules at once:

```java
Map<String, Object> rules = new HashMap<>();
rules.put("listRule", "@request.auth.id != \"\"");
rules.put("viewRule", "@request.auth.id != \"\"");
rules.put("createRule", "@request.auth.id != \"\"");
rules.put("updateRule", "@request.auth.id != \"\" && author.id ?= @request.auth.id");
rules.put("deleteRule", null);  // Only superusers

pb.collections.update("products", rules, null, null);
```

### Getting Rules

Retrieve all rules for a collection:

```java
ObjectNode collection = pb.collections.getOne("products", null, null, null, null);
System.out.println(collection.path("listRule").asText());
System.out.println(collection.path("viewRule").asText());
```

## Filter Syntax

Rules use the same filter syntax as API queries. The syntax follows: `OPERAND OPERATOR OPERAND`

### Operators

- `=` - Equal
- `!=` - NOT equal
- `>` - Greater than
- `>=` - Greater than or equal
- `<` - Less than
- `<=` - Less than or equal
- `~` - Like/Contains (auto-wraps string in `%` for wildcard)
- `!~` - NOT Like/Contains
- `?=` - Any/At least one of Equal
- `?!=` - Any/At least one of NOT equal
- `?>` - Any/At least one of Greater than
- `?>=` - Any/At least one of Greater than or equal
- `?<` - Any/At least one of Less than
- `?<=` - Any/At least one of Less than or equal
- `?~` - Any/At least one of Like/Contains
- `?!~` - Any/At least one of NOT Like/Contains

### Logical Operators

- `&&` - AND
- `||` - OR
- `(...)` - Grouping parentheses

### Field Access

#### Collection Schema Fields

Access fields from your collection schema:

```java
// Filter by status field
String filter = "status = \"active\"";

// Access nested relation fields
String filter = "author.status != \"banned\"";

// Access relation IDs
String filter = "author.id ?= @request.auth.id";
```

#### Request Context (`@request.*`)

Access current request data:

```java
// Authentication state
String rule = "@request.auth.id != \"\"";  // User is authenticated
String rule = "@request.auth.id = \"\"";  // User is guest

// Request context
String rule = "@request.context != \"oauth2\"";  // Not an OAuth2 request

// HTTP method
String rule = "@request.method = \"GET\"";

// Request headers (normalized: lowercase, "-" replaced with "_")
String rule = "@request.headers.x_token = \"test\"";

// Query parameters
String rule = "@request.query.page = \"1\"";

// Body parameters
String rule = "@request.body.title != \"\"";
```

#### Other Collections (`@collection.*`)

Target other collections that share common field values:

```java
// Check if user has access in related collection
String rule = "@collection.permissions.user ?= @request.auth.id && @collection.permissions.resource = id";
```

### Field Modifiers

#### `:isset` Modifier

Check if a request field was submitted:

```java
// Prevent changing role field
String rule = "@request.body.role:isset = false";
```

#### `:length` Modifier

Check the number of items in an array field:

```java
// At least 2 items in select field
String rule = "@request.body.tags:length > 1";

// Check existing relation field length
String rule = "someRelationField:length = 2";
```

#### `:each` Modifier

Apply condition to each item in a multiple field:

```java
// All select options contain "create"
String rule = "@request.body.someSelectField:each ~ \"create\"";

// All fields have "pb_" prefix
String rule = "someSelectField:each ~ \"pb_%\"";
```

#### `:lower` Modifier

Perform case-insensitive string comparisons:

```java
// Case-insensitive title check
String rule = "@request.body.title:lower = \"test\"";

// Case-insensitive existing field match
String rule = "title:lower ~ \"test\"";
```

### DateTime Macros

All macros are UTC-based:

```java
// Current datetime
String macro = "@now";

// Date components
String macro = "@second";    // 0-59
String macro = "@minute";    // 0-59
String macro = "@hour";      // 0-23
String macro = "@weekday";   // 0-6
String macro = "@day";       // Day number
String macro = "@month";     // Month number
String macro = "@year";      // Year number

// Relative dates
String macro = "@yesterday";
String macro = "@tomorrow";
String macro = "@todayStart";  // Beginning of current day
String macro = "@todayEnd";    // End of current day
String macro = "@monthStart";  // Beginning of current month
String macro = "@monthEnd";    // End of current month
String macro = "@yearStart";   // Beginning of current year
String macro = "@yearEnd";     // End of current year
```

Example:

```java
String rule = "@request.body.publicDate >= @now";
String rule = "created >= @todayStart && created <= @todayEnd";
```

### Functions

#### `geoDistance(lonA, latA, lonB, latB)`

Calculate Haversine distance between two geographic points in kilometres:

```java
// Offices within 25km
String rule = "geoDistance(address.lon, address.lat, 23.32, 42.69) < 25";
```

## Common Examples

### Allow Only Registered Users

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != \"\"");
pb.collections.update("products", updateData, null, null);
```

### Filter by Status

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "status = \"active\"");
pb.collections.update("products", updateData, null, null);
```

### Combine Conditions

```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != \"\" && (status = \"active\" || status = \"pending\")");
pb.collections.update("products", updateData, null, null);
```

### Filter by Relation

```java
// Only show records where user is the author
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "@request.auth.id != \"\" && author.id ?= @request.auth.id");
pb.collections.update("posts", updateData, null, null);

// Only show records where user is in allowed_users relation
Map<String, Object> updateData2 = new HashMap<>();
updateData2.put("listRule", "@request.auth.id != \"\" && allowed_users.id ?= @request.auth.id");
pb.collections.update("documents", updateData2, null, null);
```

### Public Access with Filter

```java
// Allow anyone, but only show active items
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "status = \"active\"");
pb.collections.update("products", updateData, null, null);

// Allow anyone, filter by title prefix
Map<String, Object> updateData2 = new HashMap<>();
updateData2.put("listRule", "title ~ \"Lorem%\"");
pb.collections.update("articles", updateData2, null, null);
```

### Owner-Based Update/Delete

```java
// Users can only update/delete their own records
Map<String, Object> updateData = new HashMap<>();
updateData.put("updateRule", "@request.auth.id != \"\" && author.id = @request.auth.id");
pb.collections.update("posts", updateData, null, null);

Map<String, Object> deleteData = new HashMap<>();
deleteData.put("deleteRule", "@request.auth.id != \"\" && author.id = @request.auth.id");
pb.collections.update("posts", deleteData, null, null);
```

### Prevent Field Modification

```java
// Prevent changing role field
Map<String, Object> updateData = new HashMap<>();
updateData.put("updateRule", "@request.auth.id != \"\" && @request.body.role:isset = false");
pb.collections.update("users", updateData, null, null);
```

### Date-Based Rules

```java
// Only show future events
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "startDate >= @now");
pb.collections.update("events", updateData, null, null);

// Only show items created today
Map<String, Object> updateData2 = new HashMap<>();
updateData2.put("listRule", "created >= @todayStart && created <= @todayEnd");
pb.collections.update("posts", updateData2, null, null);
```

### Array Field Validation

```java
// Require at least one tag
Map<String, Object> updateData = new HashMap<>();
updateData.put("createRule", "@request.body.tags:length > 0");
pb.collections.update("posts", updateData, null, null);

// Require all tags to start with "pb_"
Map<String, Object> updateData2 = new HashMap<>();
updateData2.put("createRule", "@request.body.tags:each ~ \"pb_%\"");
pb.collections.update("posts", updateData2, null, null);
```

### Geographic Distance

```java
// Only show offices within 25km of location
Map<String, Object> updateData = new HashMap<>();
updateData.put("listRule", "geoDistance(address.lon, address.lat, 23.32, 42.69) < 25");
pb.collections.update("offices", updateData, null, null);
```

## Best Practices

1. **Start with locked rules** (null) for security, then gradually open access as needed
2. **Use relation checks** for owner-based access patterns
3. **Combine multiple conditions** using `&&` and `||` for complex scenarios
4. **Test rules thoroughly** before deploying to production
5. **Document your rules** in code comments explaining the business logic
6. **Use empty string (`\"\"`)** only when you truly want public access
7. **Leverage modifiers** (`:isset`, `:length`, `:each`) for validation

## Error Responses

API Rules also act as data filters. When a request doesn't satisfy a rule:

- **listRule** - Returns `200` with empty items (filters out records)
- **createRule** - Returns `400` Bad Request
- **viewRule** - Returns `404` Not Found
- **updateRule** - Returns `404` Not Found
- **deleteRule** - Returns `404` Not Found
- **All rules** - Return `403` Forbidden if locked (null) and user is not superuser

## Notes

- **Superusers bypass all rules** - Rules are ignored when the action is performed by an authorized superuser
- **Rules are evaluated server-side** - Client-side validation is not enough
- **Comments are supported** - Use `//` for single-line comments in rules
- **System fields protection** - Some fields may be protected regardless of rules

