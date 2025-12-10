# Logs API - Java SDK Documentation

`pb.logs` gives superusers access to system logs for debugging and auditing.

## List logs

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

ObjectNode page = pb.logs.getList(1, 50, "level = 'error'", "-created", null, null);

System.out.println(page.path("items")); // array of log entries
```

## Fetch single log entry

```java
ObjectNode entry = pb.logs.getOne("LOG_ID", null, null);
System.out.println(entry.path("message").asText());
```

## Fetch aggregated stats

```java
List<Object> stats = pb.logs.getStats(Map.of("group", "level"), null);
stats.forEach(System.out::println);
```

Logs are read-only; use filters and sorting to focus on the events you need.
