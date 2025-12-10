# Crons API - Java SDK Documentation

The Cron API lets superusers inspect scheduled jobs and trigger them manually. Use `pb.crons` to interact with cron jobs.

## List cron jobs

```java
import com.bosbase.sdk.BosBase;
import com.fasterxml.jackson.databind.node.ObjectNode;

BosBase pb = new BosBase("http://127.0.0.1:8090");
pb.admins().authWithPassword("root@example.com", "hunter2", null, null, null, null, null, null, null, null);

List<ObjectNode> jobs = pb.crons.getFullList(null, null);

for (ObjectNode job : jobs) {
    System.out.println(job.path("name").asText() + " -> " + job.path("schedule").asText());
}
```

## Run a cron immediately

```java
// Trigger a job by id or name
pb.crons.run("nightly-cleanup", null, null, null);
```

The call executes the job now and keeps its configured schedule unchanged.
