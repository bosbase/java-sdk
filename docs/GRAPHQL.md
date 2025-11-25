# GraphQL queries with the Java SDK

Use `pb.graphql.query()` to call `/api/graphql` with your current auth token. It returns `{ data, errors, extensions }`.

> Authentication: the GraphQL endpoint is **superuser-only**. Authenticate as a superuser before calling GraphQL, e.g. `pb.admins().authWithPassword(email, password, ...)`.

## Single-table query

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

String query = """
    query ActiveUsers($limit: Int!) {
      records(collection: "users", perPage: $limit, filter: "status = true") {
        items { id data }
      }
    }
    """;

Map<String, Object> variables = new HashMap<>();
variables.put("limit", 5);

ObjectNode result = pb.graphql.query(query, variables);
ObjectNode data = (ObjectNode) result.get("data");
ArrayNode items = (ArrayNode) data.path("records").path("items");
```

## Multi-table join via expands

```java
String query = """
    query PostsWithAuthors {
      records(
        collection: "posts",
        expand: ["author", "author.profile"],
        sort: "-created"
      ) {
        items {
          id
          data  // expanded relations live under data.expand
        }
      }
    }
    """;

ObjectNode result = pb.graphql.query(query, null);
ObjectNode data = (ObjectNode) result.get("data");
ArrayNode items = (ArrayNode) data.path("records").path("items");
```

## Conditional query with variables

```java
String query = """
    query FilteredOrders($minTotal: Float!, $state: String!) {
      records(
        collection: "orders",
        filter: "total >= $minTotal && status = $state",
        sort: "created"
      ) {
        items { id data }
      }
    }
    """;

Map<String, Object> variables = new HashMap<>();
variables.put("minTotal", 100.0);
variables.put("state", "paid");

ObjectNode result = pb.graphql.query(query, variables);
```

Use the `filter`, `sort`, `page`, `perPage`, and `expand` arguments to mirror REST list behavior while keeping query logic in GraphQL.

## Create a record

```java
String mutation = """
    mutation CreatePost($data: JSON!) {
      createRecord(collection: "posts", data: $data, expand: ["author"]) {
        id
        data
      }
    }
    """;

Map<String, Object> data = new HashMap<>();
data.put("title", "Hello");
data.put("author", "USER_ID");

Map<String, Object> variables = new HashMap<>();
variables.put("data", data);

ObjectNode result = pb.graphql.query(mutation, variables);
ObjectNode createResult = (ObjectNode) result.path("data").path("createRecord");
```

## Update a record

```java
String mutation = """
    mutation UpdatePost($id: ID!, $data: JSON!) {
      updateRecord(collection: "posts", id: $id, data: $data) {
        id
        data
      }
    }
    """;

Map<String, Object> variables = new HashMap<>();
variables.put("id", "POST_ID");
Map<String, Object> data = new HashMap<>();
data.put("title", "Updated title");
variables.put("data", data);

ObjectNode result = pb.graphql.query(mutation, variables);
```

## Delete a record

```java
String mutation = """
    mutation DeletePost($id: ID!) {
      deleteRecord(collection: "posts", id: $id)
    }
    """;

Map<String, Object> variables = new HashMap<>();
variables.put("id", "POST_ID");

ObjectNode result = pb.graphql.query(mutation, variables);
```

## Complete Example

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;

public class GraphQLExample {
    public static void main(String[] args) throws Exception {
        BosBase pb = new BosBase("http://localhost:8090");
        
        // Authenticate as superuser (required for GraphQL)
        pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);
        
        // Query with variables
        String query = """
            query GetPosts($page: Int!, $perPage: Int!) {
              records(
                collection: "posts",
                page: $page,
                perPage: $perPage,
                filter: "published = true",
                sort: "-created"
              ) {
                items {
                  id
                  data
                }
                totalItems
                totalPages
              }
            }
            """;
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("page", 1);
        variables.put("perPage", 10);
        
        ObjectNode result = pb.graphql.query(query, variables);
        
        // Handle errors
        if (result.has("errors")) {
            ArrayNode errors = (ArrayNode) result.get("errors");
            for (JsonNode error : errors) {
                System.err.println("GraphQL Error: " + error);
            }
            return;
        }
        
        // Process data
        ObjectNode data = (ObjectNode) result.get("data");
        ObjectNode records = (ObjectNode) data.get("records");
        ArrayNode items = (ArrayNode) records.get("items");
        
        System.out.println("Total items: " + records.get("totalItems").asInt());
        
        for (JsonNode item : items) {
            System.out.println("Post ID: " + item.get("id").asText());
            System.out.println("Post data: " + item.get("data"));
        }
    }
}
```

