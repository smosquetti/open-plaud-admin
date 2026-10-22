# open-plaud-admin

REST API for the admin panel of a chat/call application. It keeps a PostgreSQL
database as the source of truth for **Operators** (XMPP users), **Groups**
(ejabberd MUC rooms), and memberships, while mirroring every change into an
**ejabberd** XMPP server through its `mod_http_api`.

- **Operator** — a registered client/user on the XMPP server.
- **Group** — a persistent, members-only MUC room on the XMPP server.
- **Membership** — an operator added to a group becomes an affiliated MUC
  member.
- **Temp room** — a per-conversation MUC room; operators of the given group
  are made admins so they can transfer the call, and only the joining
  operator keeps access (everyone else is `outcast`).

## Tech stack

- Kotlin + Spring Boot 4 (Spring Web, Data JPA, Validation)
- Java 17 toolchain, built with Gradle (Kotlin DSL)
- PostgreSQL
- springdoc-openapi (Swagger UI)
- ejabberd 26 (`mod_http_api`, XML-RPC is not used)

## Prerequisites

- **PostgreSQL** — database `openplaud` (defaults: `admin`/`admin`).
  `spring.jpa.hibernate.ddl-auto=validate`, so the schema must exist
  before starting the app:

  ```sql
  CREATE TABLE operators (
      id UUID PRIMARY KEY,
      name VARCHAR NOT NULL,
      username VARCHAR NOT NULL,
      email VARCHAR NOT NULL
  );
  CREATE TABLE groups (
      id UUID PRIMARY KEY,
      name VARCHAR NOT NULL,
      node VARCHAR NOT NULL,
      service VARCHAR NOT NULL
  );
  CREATE TABLE operator_groups (
      operator_id UUID NOT NULL REFERENCES operators (id),
      group_id UUID NOT NULL REFERENCES groups (id),
      PRIMARY KEY (operator_id, group_id)
  );
  ```

- **ejabberd** with `mod_http_api` enabled, listening on port `5280`,
  a `conference` MUC service registered, and an `admin@localhost`/`admin`
  account. `api_permissions` for the http access should be
  `who: admin` — loopback IP ACLs do not work when the app runs in a
  container (requests arrive from the Docker bridge).

## Configuration

All configuration lives in `src/main/resources/application.properties`:

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/openplaud` | PostgreSQL |
| `spring.datasource.username` / `.password` | `admin` / `admin` | PostgreSQL |
| `ejabberd.base-url` | `http://localhost:5280/api` | `mod_http_api` endpoint |
| `ejabberd.host` | `localhost` | XMPP host part of JIDs |
| `ejabberd.muc-service` | `conference` | Default MUC service |
| `ejabberd.username` / `ejabberd.password` | `admin@localhost` / `admin` | Admin account for room management |

## Running

```bash
./gradlew bootRun          # start the app
./gradlew test             # run the unit/integration tests
./gradlew build            # full build
```

Once running:

- API base: `http://localhost:8080/api/v1`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## API

All endpoints live under `/api/v1`.

### Operators

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/operators` | Register an operator: creates the XMPP user and an `operators` row |
| `DELETE` | `/operators/{operatorId}` | Remove an operator (membership rows + XMPP user) |

`POST /operators` request:

```json
{ "name": "John Doe", "email": "john@example.com", "password": "optional" }
```

`password` is optional — when omitted the server generates one and returns
it (once) in the 201 response:

```json
{ "id": "…", "name": "John Doe", "email": "john@example.com", "xmppUser": "johndoe", "initialPassword": "…" }
```

The XMPP user is the slug of `name`: lowercased, alphanumeric only,
2–30 characters (a valid XMPP localpart). Names that do not produce a valid
localpart are rejected.

### Groups

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/groups` | Create a group: persistent MUC room + `groups` row |
| `POST` | `/groups/{groupId}/operators/{operatorId}` | Add operator to group (MUC affiliation `member`) |
| `DELETE` | `/groups/{groupId}/operators/{operatorId}` | Remove operator from group (affiliation `none`) |
| `DELETE` | `/groups/{groupId}` | Remove group (destroys the MUC room, deletes membership rows) |

`POST /groups` request: `{ "name": "Support Team", "service": "conference" }`
→ 201 `{ "id": "…", "name": "Support Team", "room": "supportteam", "service": "conference" }`

### Temp rooms

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/chatroom` | Create a temp MUC room for `user` (slug of the username), affiliated `member`; every operator of `groupId` is set to `admin` |
| `POST` | `/join` | Operator joins the room: all *other* operators of the group are set to `outcast` |

`POST /chatroom` request: `{ "user": "jane", "groupId": "…", "service": "conference" }`
→ 201 `{ "room": "jane", "service": "conference" }`

`POST /join` request: `{ "operatorId": "…", "groupId": "…", "room": "jane" }` → 200

`service` is optional in both requests and defaults to `ejabberd.muc-service`.
Temp room endpoints do not require the caller to be authenticated; the
ejabberd admin account from the config is used.

### Errors

Failures are mapped to a consistent JSON body (`{status, code, message, fields?}`)
by a global exception handler: `404 Not Found`, `409 Conflict` (duplicate
slugs/memberships), `422 Unprocessable Entity` (invalid slug), `400` for
validation errors, and upstream ejabberd failures proxied with the original
status code.

## Consistency model

Every operation pairs a remote ejabberd call with a local database write.
The database is authoritative: if the database write fails after a
successful ejabberd call, the remote change is compensated
(unregister user / destroy room / remove affiliation), and the error is
propagated. If compensation itself fails, the log marks the remote entity
as orphaned so it can be cleaned up manually.

## Project layout

```
src/main/kotlin/dev/stevanmosquetti/openplaud/
├── OpenplaudApplication.kt      # Spring Boot entry point
├── web/                          # REST controllers + global exception handler
├── service/                      # Business logic (Operator, Group, TempRoom)
├── client/                       # Thin client over ejabberd mod_http_api
├── entity/                       # JPA entities (operators, groups, operator_groups)
├── repository/                   # Spring Data repositories
├── dto/                          # Request/response DTOs
├── slugify/                      # Display name -> XMPP localpart
└── config/                       # OpenAPI config
```

Tests live in `src/test/kotlin` (unit + integration, H2 for tests).
