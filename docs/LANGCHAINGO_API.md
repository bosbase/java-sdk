# LangChaingo API - Java SDK Documentation

BosBase exposes the `/api/langchaingo` endpoints so you can run LangChainGo powered workflows without leaving the platform. The Java SDK wraps these endpoints with the `pb.langchaingo` service.

The service exposes four high-level methods:

| Method | HTTP Endpoint | Description |
| --- | --- | --- |
| `pb.langchaingo.completions()` | `POST /api/langchaingo/completions` | Runs a chat/completion call using the configured LLM provider. |
| `pb.langchaingo.rag()` | `POST /api/langchaingo/rag` | Runs a retrieval-augmented generation pass over an `llmDocuments` collection. |
| `pb.langchaingo.queryDocuments()` | `POST /api/langchaingo/documents/query` | Asks an OpenAI-backed chain to answer questions over `llmDocuments` and optionally return matched sources. |
| `pb.langchaingo.sql()` | `POST /api/langchaingo/sql` | Lets OpenAI draft and execute SQL against your BosBase database, then returns the results. |

Each method accepts an optional `model` configuration:

```java
Map<String, Object> modelConfig = new HashMap<>();
modelConfig.put("provider", "openai");  // or "ollama" or other provider
modelConfig.put("model", "gpt-4o-mini");
modelConfig.put("apiKey", "your-api-key");  // Optional, overrides server defaults
modelConfig.put("baseUrl", "https://api.openai.com/v1");  // Optional
```

If you omit the `model` section, BosBase defaults to `provider: "openai"` and `model: "gpt-4o-mini"` with credentials read from the server environment. Passing an `apiKey` lets you override server defaults on a per-request basis.

## Text + Chat Completions

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

BosBase pb = new BosBase("http://localhost:8090");

Map<String, Object> modelConfig = new HashMap<>();
modelConfig.put("provider", "openai");
modelConfig.put("model", "gpt-4o-mini");

List<Map<String, Object>> messages = new ArrayList<>();
Map<String, Object> systemMsg = new HashMap<>();
systemMsg.put("role", "system");
systemMsg.put("content", "Answer in one sentence.");
messages.add(systemMsg);

Map<String, Object> userMsg = new HashMap<>();
userMsg.put("role", "user");
userMsg.put("content", "Explain Rayleigh scattering.");
messages.add(userMsg);

Map<String, Object> request = new HashMap<>();
request.put("model", modelConfig);
request.put("messages", messages);
request.put("temperature", 0.2);

ObjectNode completion = pb.langchaingo.completions(request);
System.out.println(completion.get("content").asText());
```

The completion response mirrors the LangChainGo `ContentResponse` shape, so you can inspect the `functionCall`, `toolCalls`, or `generationInfo` fields when you need more than plain text.

## Retrieval-Augmented Generation (RAG)

Pair the LangChaingo endpoints with the `/api/llm-documents` store to build RAG workflows. The backend automatically uses the chromem-go collection configured for the target LLM collection.

```java
Map<String, Object> ragRequest = new HashMap<>();
ragRequest.put("collection", "knowledge-base");
ragRequest.put("question", "Why is the sky blue?");
ragRequest.put("topK", 4);
ragRequest.put("returnSources", true);

Map<String, Object> filters = new HashMap<>();
Map<String, Object> where = new HashMap<>();
where.put("topic", "physics");
filters.put("where", where);
ragRequest.put("filters", filters);

ObjectNode answer = pb.langchaingo.rag(ragRequest);
System.out.println(answer.get("answer").asText());

ArrayNode sources = (ArrayNode) answer.get("sources");
if (sources != null) {
    for (JsonNode source : sources) {
        System.out.println("Score: " + source.get("score").asDouble());
        System.out.println("Title: " + source.path("metadata").path("title").asText());
    }
}
```

Set `promptTemplate` when you want to control how the retrieved context is stuffed into the answer prompt:

```java
Map<String, Object> ragRequest = new HashMap<>();
ragRequest.put("collection", "knowledge-base");
ragRequest.put("question", "Summarize the explanation below in 2 sentences.");
ragRequest.put("promptTemplate", "Context:\\n{{.context}}\\n\\nQuestion: {{.question}}\\nSummary:");

ObjectNode answer = pb.langchaingo.rag(ragRequest);
```

## LLM Document Queries

> **Note**: This interface is only available to superusers.

When you want to pose a question to a specific `llmDocuments` collection and have LangChaingo+OpenAI synthesize an answer, use `queryDocuments`. It mirrors the RAG arguments but takes a `query` field:

```java
// Authenticate as superuser first
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

Map<String, Object> queryRequest = new HashMap<>();
queryRequest.put("collection", "knowledge-base");
queryRequest.put("query", "List three bullet points about Rayleigh scattering.");
queryRequest.put("topK", 3);
queryRequest.put("returnSources", true);

ObjectNode response = pb.langchaingo.queryDocuments(queryRequest);
System.out.println(response.get("answer").asText());
System.out.println(response.get("sources"));
```

## SQL Generation + Execution

> **Important Notes**:
> - This interface is only available to superusers. Requests authenticated with regular `users` tokens return a `401 Unauthorized`.
> - It is recommended to execute query statements (SELECT) only.
> - **Do not use this interface for adding or modifying table structures.** Collection interfaces should be used instead for managing database schema.
> - Directly using this interface for initializing table structures and adding or modifying database tables will cause errors that prevent the automatic generation of APIs.

Superuser tokens (`_superusers` records) can ask LangChaingo to have OpenAI propose a SQL statement, execute it, and return both the generated SQL and execution output.

```java
// Authenticate as superuser
pb.admins().authWithPassword("admin@example.com", "password", null, null, null, null, null, null, null, null);

Map<String, Object> sqlRequest = new HashMap<>();
sqlRequest.put("query", "Add a demo project row if it doesn't exist, then list the 5 most recent projects.");
sqlRequest.put("tables", List.of("projects"));  // optional hint to limit which tables the model sees
sqlRequest.put("topK", 5);

ObjectNode result = pb.langchaingo.sql(sqlRequest);
System.out.println(result.get("sql").asText());    // Generated SQL
System.out.println(result.get("answer").asText()); // Model's summary of the execution
System.out.println(result.get("columns"));
System.out.println(result.get("rows"));
```

Use `tables` to restrict which table definitions and sample rows are passed to the model, and `topK` to control how many rows the model should target when building queries. You can also pass the optional `model` block described above to override the default OpenAI model or key for this call.

## Complete Example

```java
import com.bosbase.sdk.BosBase;
import java.util.*;

public class LangChaingoExample {
    public static void main(String[] args) throws Exception {
        BosBase pb = new BosBase("http://localhost:8090");
        
        // Example 1: Chat completion
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("provider", "openai");
        modelConfig.put("model", "gpt-4o-mini");
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "What is the capital of France?");
        messages.add(userMsg);
        
        Map<String, Object> completionRequest = new HashMap<>();
        completionRequest.put("model", modelConfig);
        completionRequest.put("messages", messages);
        
        ObjectNode completion = pb.langchaingo.completions(completionRequest);
        System.out.println("Completion: " + completion.get("content").asText());
        
        // Example 2: RAG query
        Map<String, Object> ragRequest = new HashMap<>();
        ragRequest.put("collection", "knowledge-base");
        ragRequest.put("question", "Why is the sky blue?");
        ragRequest.put("topK", 3);
        ragRequest.put("returnSources", true);
        
        ObjectNode ragAnswer = pb.langchaingo.rag(ragRequest);
        System.out.println("RAG Answer: " + ragAnswer.get("answer").asText());
    }
}
```

