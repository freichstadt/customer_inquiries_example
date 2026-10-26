# Customer inquiries API


## Project layout

```text
src/main/java/com/example/inquiries/
  InquiryApplication.java
  repository/  # PostgreSQL queries using jOOQ
  data/        # DTO, POJO and database records
  exception/   # Exceptions and error mapping
  service/     # Application service
  rest/        # HTTP endpoints, request validation
src/main/resources/
  application.yaml
  db/migration/V1__create_customer_inquiries.sql
```
Request flow: `rest -> service -> repository -> PostgreSQL`.


## Start locally

Prerequisites: a JDK 27 installation and Docker with Docker Compose v2. The Maven wrapper downloads Maven 3.9.11; no separate Maven installation is needed. The first build requires internet access. Set `JAVA_HOME` to your JDK if Java is not already configured.

Run these commands from this directory:

```sh
docker compose up -d --wait postgres
./mvnw spring-boot:run
```

The API listens at `http://localhost:8080`. Flyway automatically creates the `customer_inquiries` table on startup. Docker Compose starts PostgreSQL; Java runs on your host. PostgreSQL development data persists in a named Docker volume.

You can also build and run a JAR:

```sh
./mvnw package
java -jar target/customer-inquiries-0.0.1-SNAPSHOT.jar
```
Windows: use `mvnw.cmd` instead of `./mvnw`.


After trying the API, you can take down the docker compose and the local volume:
```sh
docker compose --profile test down -v
```

## Try the API

```sh
# Create (201 Created, Location header, initial status NEW)
curl -i -X POST http://localhost:8080/api/inquiries \
  -H 'Content-Type: application/json' \
  -d '{"name":"Fab","message":"I need help with my order"}'

# First page (200 OK, default limit 10)
curl http://localhost:8080/api/inquiries

# Get one: copy the UUID from the create response
INQUIRY_ID="7635b748-cd90-4a0d-8ee9-e3c3c6c1618d"
curl "http://localhost:8080/api/inquiries/$INQUIRY_ID"

# Update status (200 OK)
curl -i -X PATCH "http://localhost:8080/api/inquiries/$INQUIRY_ID/status" \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
```

## Cursor pagination

`GET /api/inquiries?limit=10&cursor=<token>` returns newest inquiries first in a page object:

```json
{
  "items": [{"id": "7635b748-cd90-4a0d-8ee9-e3c3c6c1618d", "name": "Fab", "message": "Help", "status": "NEW", "createdAt": "2026-01-02T12:00:00Z"}],
  "nextCursor": "MjAyNi0wMS0wMlQxMjowMDowMFp8NzYzNWI3NDgtY2Q5MC00YTBkLThlZTktZTNjM2M2YzE2MThk"
}
```

Omit `cursor` for the first page. Pass `nextCursor` unchanged on the next request. It is null when there is no further page, including an empty result. 
```sh
curl 'http://localhost:8080/api/inquiries?limit=10'
# Set CURSOR to the nextCursor returned by the API:
CURSOR="MjAyNi0wMS0wMlQxMjowMDowMFp8NzYzNWI3NDgtY2Q5MC00YTBkLThlZTktZTNjM2M2YzE2MThk"
curl "http://localhost:8080/api/inquiries?limit=10&cursor=$CURSOR"
```

## Tests

Service unit tests and MVC tests run without PostgreSQL:

```sh
./mvnw test
```

Integration tests use a **separate PostgreSQL container** on port **5433**, database `inquiries_test`, with temporary storage. The normal app uses port **5432**, database `inquiries`.

```sh
docker compose --profile test up -d --wait postgres-test
./mvnw -Pintegration verify
```

To inspect the development table:

```sh
docker compose exec postgres psql -U inquiries -d inquiries \
  -c 'SELECT * FROM customer_inquiries ORDER BY id;'
```

Stop both containers (preserves the development volume):

```sh
docker compose --profile test down
```


## jOOQ code generation

Maven generates jOOQ classes from the initial SQL schema during `generate-sources`. `./mvnw test`, `./mvnw package`, and `./mvnw spring-boot:run` will generate them automatically. But you can generate them manually using:

```sh
./mvnw generate-sources
```

## Future improvements

 - Any transition between NEW, IN_PROGRESS, and CLOSED is allowed, including reopening a closed inquiry. The last update is the one that win. Need a more defined flow. 
 - There is no authentication or authorization, so anyone with access to the API can read all inquiries and update their status. Need authentication and ownership or role-based authorization.
 - There is no history of changes stored: record who changed a status and when
 - Can have conflicting update of status. Need locking (pessimistic or optimistic)
 - OpenAPI documentation
 - Run the automated tests in CI (Jenkins or similar)
