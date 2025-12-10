# SQL Execution API - Java SDK Documentation

Superusers can run ad-hoc SQL against the BosBase database through `pb.sql.execute`.

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

ObjectNode result = pb.sql.execute(
    "SELECT id, email FROM _users WHERE created >= NOW() - INTERVAL '1 day'",
    null,
    null,
    null
);

System.out.println(result);
```

The endpoint returns the raw execution payload from the server (rows, columns, and metadata). Only superusers are allowed to call it.
