# Courier Tracking API

Java 21 / Spring Boot service that ingests courier GPS points, accumulates travel distance, and records store entrance events.

## Requirements

- Java 21
- Docker (PostgreSQL for local run, and Testcontainers for tests)
- Maven Wrapper (`./mvnw` / `mvnw.cmd`) — no system Maven required

## Run

Postgres only, then the app on the host (port 8080):

```bash
docker compose up -d postgres
./mvnw spring-boot:run
./mvnw test
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`.

`./mvnw test` uses Testcontainers PostgreSQL and does not need Compose or a running app.

### All-in-Docker

Build the app image and start Postgres + API together:

```bash
docker compose up --build
```

API: `http://localhost:8080`

## Sample requests

Replace `localhost:8080` if the app is bound elsewhere. Ataşehir MMM Migros is at `40.9923307, 29.1244229`.

**Ingest** (first point inside the store → `201`, `applied=true`, one entrance event):

```bash
curl -s -D - -X POST http://localhost:8080/api/v1/locations \
  -H "Content-Type: application/json" \
  -d "{\"courierId\":\"c1\",\"timestamp\":\"2024-01-01T00:01:00Z\",\"latitude\":40.9923307,\"longitude\":29.1244229}"
```

**Duplicate retry** (same courier, timestamp, and coordinates → `200`, no side effects):

```bash
curl -s -D - -X POST http://localhost:8080/api/v1/locations \
  -H "Content-Type: application/json" \
  -d "{\"courierId\":\"c1\",\"timestamp\":\"2024-01-01T00:01:00Z\",\"latitude\":40.9923307,\"longitude\":29.1244229}"
```

**Out-of-order** (earlier timestamp → `201`, `applied=false`, distance and geofence unchanged):

```bash
curl -s -D - -X POST http://localhost:8080/api/v1/locations \
  -H "Content-Type: application/json" \
  -d "{\"courierId\":\"c1\",\"timestamp\":\"2024-01-01T00:00:00Z\",\"latitude\":41.0,\"longitude\":29.0}"
```

**Distance query** (`404` if the courier was never ingested):

```bash
curl -s -D - http://localhost:8080/api/v1/couriers/c1/distance
```

**Entrance events** (both query params optional; ordered by `occurredAt` ascending):

```bash
curl -s "http://localhost:8080/api/v1/entrance-events?courierId=c1&storeId=Ata%C5%9Fehir%20MMM%20Migros"
```

**Stores** (loaded from `stores.json`):

```bash
curl -s http://localhost:8080/api/v1/stores
```

Errors are RFC 7807 `ProblemDetail` (`400` validation / malformed JSON / bad timestamp, `409` same key with different coordinates, `404` unknown courier).

## Architecture

A small layered modular monolith: HTTP adapters in `api` map to DTOs and Problem Details only; `LocationIngestionService` is the single `@Transactional` boundary and orchestrates lock-or-create, idempotency, out-of-order history, distance, and geofence evaluation; `domain` is pure Java (no Spring, no JPA) with swappable distance calculation and state-based geofence transitions; `infrastructure` loads an immutable `StoreCatalog` from `stores.json` at startup and persists through JPA entities that are mapped to domain models. Durable state lives in PostgreSQL so a restart is safe; stores are configuration, not tables.

```
com.example.courier
├── api                  controllers, DTO records, ProblemDetail advice
├── application          LocationIngestionService, result / conflict types
├── domain
│   ├── model            Location, Store, CourierTrack, EntranceEvent
│   ├── geo              DistanceCalculator, HaversineDistanceCalculator
│   ├── geofence         GeofenceState (Outside/Inside), GeofenceEvaluator
│   └── port             StoreCatalog + repository interfaces
└── infrastructure
    ├── store            stores.json loader
    └── persistence      JPA entities, Spring Data, adapters, mappers
```

## Design patterns

- **Strategy** (`DistanceCalculator`): the Haversine implementation is one algorithm behind an interface so tests can inject a fake and a future geodesic formula would not leak into the service.
- **State** (`GeofenceState.Outside` / `Inside`): entrance, stay, exit, and 60 s re-entry cooldown live on the state objects instead of a branching pile in `LocationIngestionService`.
- **Repository** (ports in `domain.port`, JPA adapters in `infrastructure`): persistence and locking (`INSERT … ON CONFLICT DO NOTHING` then `SELECT … FOR UPDATE`) stay behind interfaces the domain/application can call without Spring or SQL.

No other patterns were added.

## Assumptions

Interpretations of the agreed business rules (the simplest option consistent with the spec):

3. **Entrance / 100 m boundary.** A courier is inside a store iff Haversine distance `<= 100.0` m (inclusive). `99.9` and exactly `100.0` are inside; `100.1` is outside.

5. **Re-entry suppression.** Evaluated independently per `(courierId, storeId)`. An `EntranceEvent` is written only when `last_entrance_at` is null or `(recorded_at - last_entrance_at) >= 60 seconds`. If suppressed: no event, `last_entrance_at` is not advanced, but the pair still becomes `INSIDE`. Business time is always the courier-supplied `recorded_at`; `received_at` is an audit column set to server `Instant.now()` and is never used for distance, geofence, or cooldown.

6. **Initial state.** A missing `geofence_state` row means `OUTSIDE` with no previous entrance. Therefore the first *applied* location already inside a radius is an entrance.

8. **Out-of-order / non-advancing.** `recorded_at <= last_recorded_at` (equality included) is stored in `courier_location` with `applied=false` and does not change distance, geofence state, or events. Response is `201` with `applied=false`.

9. **Duplicates.** Natural idempotency key is `(courierId, timestamp)`. Same coordinates (`Double.compare` on the persisted doubles) → HTTP `200`, no writes. Response body uses the stored `applied` flag, the current track total, and entrance events for that courier whose `occurred_at` equals the timestamp (feasible reconstruction of the original outcome). Different coordinates → HTTP `409`.

10. **Future timestamps.** No upper bound. The timestamp must parse as an ISO-8601 instant.

11. **Unknown courier.** `GET /couriers/{id}/distance` returns `404` when no `courier_track` row exists (the courier has never been ingested).

**60 s boundary.** Exactly 60.000 seconds is enough to emit (`recorded_at >= last_entrance_at + 60s`). 59 seconds is not.

Store identity: `stores.json` has no `id` field, so `store_id` is the store `name`. String columns are `VARCHAR(255)` so Hibernate `ddl-auto=validate` matches JPA defaults.

## How this would scale

The current design is enough for one-node, courier-keyed row locks: same courier is serialized in Postgres, different couriers run in parallel. When ingest volume justifies it, publish an **outbox** row in the same transaction as the location write, drain it with a **message broker**, and **partition consumers by courierId** so each courier’s stream stays ordered. Do not introduce a broker, cache, or extra store until that load shows up; premature partitioning would only move the same lock problem.

## Tests

```bash
./mvnw test
./mvnw clean verify
```

- Unit tests (no Spring, no Docker): Haversine, geofence transitions/cooldown, distance accumulation, ingestion orchestration with in-memory fakes.
- `@WebMvcTest`: HTTP status codes, validation messages, malformed JSON, bad timestamps.
- Testcontainers PostgreSQL: schema/persistence, ingestion duplicates and rollback, concurrent POSTs.

Docker must be running for Testcontainers.
