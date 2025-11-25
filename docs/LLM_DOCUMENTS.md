# LLM Document API - Java SDK Documentation

The `LLMDocumentService` wraps the `/api/llm-documents` endpoints that are backed by the embedded chromem-go vector store (persisted in rqlite). Each document contains text content, optional metadata and an embedding vector that can be queried with semantic search.

## Getting Started

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");

// create a logical namespace for your documents
Map<String, Object> options = new HashMap<>();
options.put("domain", "internal");
pb.llmDocuments.createCollection("knowledge-base", options);
```

## Insert Documents

```java
// Insert document without ID (auto-generated)
Map<String, Object> doc = new HashMap<>();
doc.put("content", "Leaves are green because chlorophyll absorbs red and blue light.");
Map<String, Object> metadata = new HashMap<>();
metadata.put("topic", "biology");
doc.put("metadata", metadata);

Map<String, Object> insertOptions = new HashMap<>();
insertOptions.put("collection", "knowledge-base");
ObjectNode result = pb.llmDocuments.insert(doc, insertOptions);

// Insert document with custom ID
Map<String, Object> doc2 = new HashMap<>();
doc2.put("id", "sky");
doc2.put("content", "The sky is blue because of Rayleigh scattering.");
Map<String, Object> metadata2 = new HashMap<>();
metadata2.put("topic", "physics");
doc2.put("metadata", metadata2);

ObjectNode result2 = pb.llmDocuments.insert(doc2, insertOptions);
```

## Query Documents

```java
Map<String, Object> query = new HashMap<>();
query.put("queryText", "Why is the sky blue?");
query.put("limit", 3);
Map<String, Object> where = new HashMap<>();
where.put("topic", "physics");
query.put("where", where);

Map<String, Object> queryOptions = new HashMap<>();
queryOptions.put("collection", "knowledge-base");

ObjectNode result = pb.llmDocuments.query(query, queryOptions);

ArrayNode results = (ArrayNode) result.get("results");
for (JsonNode match : results) {
    System.out.println("ID: " + match.get("id").asText());
    System.out.println("Similarity: " + match.get("similarity").asDouble());
}
```

## Manage Documents

```java
// update a document
Map<String, Object> updateData = new HashMap<>();
Map<String, Object> metadata = new HashMap<>();
metadata.put("topic", "physics");
metadata.put("reviewed", "true");
updateData.put("metadata", metadata);

Map<String, Object> updateOptions = new HashMap<>();
updateOptions.put("collection", "knowledge-base");
pb.llmDocuments.update("sky", updateData, updateOptions);

// list documents with pagination
Map<String, Object> listOptions = new HashMap<>();
listOptions.put("collection", "knowledge-base");
listOptions.put("page", 1);
listOptions.put("perPage", 25);
ObjectNode page = pb.llmDocuments.list(listOptions);

// delete unwanted entries
Map<String, Object> deleteOptions = new HashMap<>();
deleteOptions.put("collection", "knowledge-base");
pb.llmDocuments.delete("sky", deleteOptions);
```

## HTTP Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET /api/llm-documents/collections` | List collections |
| `POST /api/llm-documents/collections/{name}` | Create collection |
| `DELETE /api/llm-documents/collections/{name}` | Delete collection |
| `GET /api/llm-documents/{collection}` | List documents |
| `POST /api/llm-documents/{collection}` | Insert document |
| `GET /api/llm-documents/{collection}/{id}` | Fetch document |
| `PATCH /api/llm-documents/{collection}/{id}` | Update document |
| `DELETE /api/llm-documents/{collection}/{id}` | Delete document |
| `POST /api/llm-documents/{collection}/documents/query` | Query by semantic similarity |

## Complete Example

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

public class LLMDocumentsExample {
    public static void main(String[] args) throws Exception {
        BosBase pb = new BosBase("http://localhost:8090");
        
        // Create collection
        Map<String, Object> collectionOptions = new HashMap<>();
        collectionOptions.put("domain", "internal");
        pb.llmDocuments.createCollection("knowledge-base", collectionOptions);
        
        // Insert documents
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("content", "The sky is blue because of Rayleigh scattering.");
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("topic", "physics");
        doc1.put("metadata", metadata1);
        
        Map<String, Object> insertOptions = new HashMap<>();
        insertOptions.put("collection", "knowledge-base");
        pb.llmDocuments.insert(doc1, insertOptions);
        
        // Query documents
        Map<String, Object> query = new HashMap<>();
        query.put("queryText", "Why is the sky blue?");
        query.put("limit", 3);
        
        Map<String, Object> queryOptions = new HashMap<>();
        queryOptions.put("collection", "knowledge-base");
        ObjectNode result = pb.llmDocuments.query(query, queryOptions);
        
        ArrayNode results = (ArrayNode) result.get("results");
        for (JsonNode match : results) {
            System.out.println("Match: " + match.get("id").asText());
            System.out.println("Similarity: " + match.get("similarity").asDouble());
        }
    }
}
```

