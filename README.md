# Smart Campus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** and an embedded **Grizzly HTTP server** for managing campus rooms and IoT sensors.

---

## API Overview

The API follows REST architectural principles with a versioned base path of `/api/v1`. It manages three core resources:

- **Rooms** — physical spaces on campus with a capacity and linked sensors
- **Sensors** — IoT devices (Temperature, CO2, Occupancy, etc.) installed in rooms
- **Sensor Readings** — timestamped historical measurement log per sensor

All data is stored in-memory using `ConcurrentHashMap` and `ArrayList`. No database is used.

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 11+ |
| JAX-RS Implementation | Jersey 2.39.1 |
| HTTP Server | Grizzly2 (embedded) |
| JSON | Jackson (via jersey-media-json-jackson) |
| Build Tool | Maven 3.x |

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api

# 2. Build the fat JAR (includes all dependencies)
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api.jar
```

The server starts at: **http://localhost:8080/api/v1**

Press `ENTER` in the terminal to stop the server.

---

## Endpoint Reference

| Method | Path | Description |
|---|---|---|
| GET | /api/v1 | Discovery / API metadata |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get room by ID |
| DELETE | /api/v1/rooms/{roomId} | Delete a room (blocked if sensors exist) |
| GET | /api/v1/sensors | List all sensors (optional `?type=` filter) |
| POST | /api/v1/sensors | Register a new sensor |
| GET | /api/v1/sensors/{sensorId} | Get sensor by ID |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history |
| POST | /api/v1/sensors/{sensorId}/readings | Add a new reading |
| GET | /api/v1/sensors/{sensorId}/readings/{readingId} | Get specific reading |

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a new Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "CS-101", "name": "Computer Science Lab", "capacity": 40}'
```

### 3. Register a new Sensor (linked to an existing room)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-002", "type": "Temperature", "status": "ACTIVE", "currentValue": 20.0, "roomId": "LIB-301"}'
```

### 4. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 5. Post a sensor reading (updates parent sensor's currentValue)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.7}'
```

### 6. Get reading history for a sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 7. Attempt to delete a room with sensors (triggers 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 8. Register sensor with non-existent roomId (triggers 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-999", "type": "Temperature", "status": "ACTIVE", "currentValue": 0.0, "roomId": "FAKE-999"}'
```

### 9. Post reading to a MAINTENANCE sensor (triggers 403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 5.0}'
```

### 10. Delete a sensor, then delete its now-empty room
```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/OCC-001
curl -X DELETE http://localhost:8080/api/v1/rooms/LAB-101
```

---

## Conceptual Report (Question Answers)

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new resource class instance for every incoming HTTP request** (request-scoped). This is the specification default and ensures thread isolation per request. The consequence is that any instance variables declared inside a resource class are not shared between requests — storing a `HashMap` as an instance field would mean each request starts with an empty map.

To manage shared in-memory state safely, all data must be stored in a **singleton** (`DataStore.getInstance()`). This singleton uses `ConcurrentHashMap` instead of `HashMap` because multiple simultaneous requests can read and write concurrently. `ConcurrentHashMap` provides lock-striped thread safety, preventing race conditions and data corruption without requiring explicit `synchronized` blocks on every operation.

---

### Part 1.2 — HATEOAS (Hypermedia As The Engine Of Application State)

HATEOAS is the principle of embedding navigable links within API responses so clients can discover actions and resources dynamically, without relying on hardcoded URLs or static external documentation. For example, a response for a room might include `"readings": "/api/v1/sensors/TEMP-001/readings"`.

**Benefits over static documentation:**
- **Self-documenting**: The API response itself tells the client what is possible next.
- **Decoupled clients**: If a URL changes server-side, only the server changes — clients follow the links dynamically and do not break.
- **Discoverability**: New developers can explore the API by following responses without consulting a separate doc portal.
- **Reduced coupling**: Clients depend on semantics (link relations) rather than URL structures.

---

### Part 2.1 — Returning IDs vs Full Objects in List Responses

**Returning only IDs** (e.g., `["LIB-301", "LAB-101"]`) is bandwidth-efficient and fast but forces the client to make N additional requests to fetch the details of each room — this is the "N+1 problem" and is expensive at scale.

**Returning full objects** is more practical for most clients since they receive all data in one round trip, reducing latency. The trade-off is a larger payload. The correct approach depends on context: list views typically benefit from full objects (or a summary projection), while deep nesting or very large collections may justify ID-only responses or pagination.

---

### Part 2.2 — Idempotency of DELETE

`DELETE` is **idempotent** in the HTTP specification: sending the same request multiple times must result in the same server state. In this implementation:

- **First DELETE** of a valid room → room is removed, returns `200 OK`.
- **Second DELETE** of the same room → room no longer exists, returns `404 Not Found`.

The server state (room absent) is identical after both calls, so the operation is idempotent. The response code differs (200 vs 404), but idempotency is about *state*, not *response code*. No additional side effects occur on repeated calls.

---

### Part 3.1 — @Consumes and Content-Type Mismatches

`@Consumes(MediaType.APPLICATION_JSON)` tells the JAX-RS runtime that the endpoint only accepts `application/json` request bodies. If a client sends a request with `Content-Type: text/plain` or `application/xml`, the runtime **rejects the request before invoking the method** and automatically returns **HTTP 415 Unsupported Media Type**. This is handled entirely by the JAX-RS framework — no custom code is needed. It protects the resource method from receiving data it cannot deserialise.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` (query parameter) is the correct design for filtering because:

- **Optionality**: Query parameters are naturally optional; the endpoint `/sensors` still works without them and returns all sensors.
- **Resource identity**: The resource is always the sensors collection. The `type` filter is a view concern, not an identity concern.
- **Multiple filters**: Query params compose easily (`?type=CO2&status=ACTIVE`). Path parameters cannot do this without convoluted nested paths.
- **REST semantics**: Path segments identify resources; query strings parameterise operations on those resources. Filtering is an operation, not an identifier.

`/api/v1/sensors/type/CO2` would imply that `type` is a sub-resource of sensors, which is semantically misleading and harder to extend.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern delegates the handling of a sub-path to a dedicated class at runtime, rather than defining every nested route in one large controller.

**Architectural benefits:**
- **Single Responsibility**: `SensorResource` handles sensor lifecycle; `SensorReadingResource` handles reading history. Each class has one clear job.
- **Maintainability**: In large APIs with deep nesting (e.g., `/rooms/{id}/sensors/{id}/readings/{id}/annotations`), a single controller would become unmanageable. Delegation keeps each class small and focused.
- **Testability**: Sub-resource classes can be unit-tested independently without loading the full resource hierarchy.
- **Reusability**: The same `SensorReadingResource` class could theoretically be reused by different parent resources if needed.

---

### Part 5.2 — HTTP 422 vs HTTP 404 for Missing References

`404 Not Found` means the **requested endpoint URL does not exist** on the server. Using it for a missing `roomId` inside a valid JSON body is misleading — the endpoint `/api/v1/sensors` clearly exists.

`422 Unprocessable Entity` accurately conveys that:
- The request was received and syntactically valid (correct JSON, correct Content-Type).
- The server understood the intent.
- The **semantic content** of the payload is invalid — a referenced entity (`roomId`) does not exist.

This gives client developers precise diagnostic information: "your URL and JSON format are correct, but your data references something that doesn't exist." It separates routing errors (404) from payload validation errors (422).

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security risk because they reveal:

1. **Internal class paths and package names** — attackers learn the application's internal structure, making it easier to craft targeted attacks.
2. **Library names and versions** — e.g., `org.glassfish.jersey 2.x` tells an attacker exactly which CVEs (Common Vulnerabilities and Exposures) may apply to the running software.
3. **Business logic flow** — the call stack shows which methods were invoked in what order, exposing implementation details that could be exploited.
4. **Database or file system paths** — stack traces from data access layers can leak connection strings, table names, or file paths.
5. **Configuration details** — server configuration or environment variable names can appear in exception messages.

The global `ExceptionMapper<Throwable>` prevents all of this by returning a generic `500 Internal Server Error` message while logging the full trace **server-side only** — visible to developers but never to clients.

---

### Part 5.5 — JAX-RS Filters for Cross-Cutting Concerns

Using filters (`@Provider` implementing `ContainerRequestFilter` / `ContainerResponseFilter`) is superior to placing `Logger.info()` in every resource method because:

- **DRY Principle**: Logging logic is written once and applied universally. Adding a new resource endpoint automatically inherits logging.
- **Separation of Concerns**: Resource methods focus purely on business logic; infrastructure concerns (logging, authentication, CORS headers) are handled in the filter pipeline.
- **Consistency**: Every request is logged with the same format. Manual logging risks inconsistency or omission.
- **Maintainability**: Changing the log format requires editing one class, not dozens of resource methods.
- **Order control**: JAX-RS guarantees filters run in a predictable order relative to resource method execution.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          # Embedded Grizzly server entry point
    ├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
    ├── DataStore.java                     # Singleton in-memory ConcurrentHashMap store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java         # GET /api/v1
    │   ├── RoomResource.java              # /api/v1/rooms
    │   ├── SensorResource.java            # /api/v1/sensors + sub-resource locator
    │   └── SensorReadingResource.java     # /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java     # 409 Conflict
    │   ├── LinkedResourceNotFoundException.java  # 422 Unprocessable Entity
    │   ├── SensorUnavailableException.java        # 403 Forbidden
    │   └── ExceptionMappers.java          # All @Provider mappers + global 500
    └── filter/
        └── LoggingFilter.java             # Request/response logging
```
