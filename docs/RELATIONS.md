# Relations - Java SDK Documentation

Use the `expand` query parameter to load related records when fetching data. The Java SDK passes `expand` through `RecordService` helpers.

## Expand related records

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");

ObjectNode post = pb.collection("posts").getOne(
    "RECORD_ID",
    "author,comments.author", // expand nested relations
    null,
    null,
    null
);

ObjectNode author = (ObjectNode) post.path("expand").path("author");
System.out.println(author.path("email").asText());
```

## Use filters on relations

```java
ResultList<ObjectNode> posts = pb.collection("posts").getList(
    1, 20, false,
    pb.filter("comments.author.email != {:email}", Map.of("email", "blocked@example.com")),
    "-created",
    "comments.author",
    null,
    null,
    null
);
```

Expansions can go up to six levels deep. Combine `fields` and `filter` to control payload size and performance.
