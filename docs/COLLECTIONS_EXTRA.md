# Collections Extras - Java SDK Documentation

The Java SDK exposes several helpers that wrap common collection management tasks such as scaffolding, field/index updates, and SQL-backed collections. All helpers live on `pb.collections`.

## Create collections from scaffolds

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

// Base collection scaffold
ObjectNode articles = pb.collections.createBase("articles", Map.of(
    "listRule", "@request.auth.id != ''"
), null);

// Auth collection scaffold
pb.collections.createAuth("customers", Map.of("emailRule", "@request.data.email != ''"), null);

// View collection scaffold
pb.collections.createView("recent_posts", "SELECT * FROM posts WHERE published = TRUE", null, null);
```

## Manage fields and indexes

```java
// Add a new field
pb.collections.addField("articles", Map.of(
    "name", "summary",
    "type", "text",
    "required", false
), null);

// Update an existing field
pb.collections.updateField("articles", "summary", Map.of("presentable", true), null);

// Remove a field (also cleans dependent indexes)
pb.collections.removeField("articles", "old_field", null);

// Create an index
pb.collections.addIndex("articles", List.of("created"), false, null, null);

// Drop an index that contains the listed columns
pb.collections.removeIndex("articles", List.of("created"), null);
```

## Export and import definitions

```java
// Export normalized definitions
List<ObjectNode> snapshot = pb.collections.exportCollections(null, null);

// Normalize before importing (deduplicates field ids)
List<ObjectNode> cleaned = pb.collections.normalizeForImport(snapshot);

// Import and optionally delete missing collections
pb.collections.importCollections(cleaned, true, null);
```

These helpers save you from manually crafting collection JSON definitions and keep schemas consistent across environments.
