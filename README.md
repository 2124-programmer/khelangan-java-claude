# TurfBook Backend

> **Spring Boot Version**: This project uses **Spring Boot 3.5.1** (Spring Framework 6.2.x, Jakarta EE 10, Java 21).
> Spring Boot 4.x does not yet exist — do not attempt to upgrade to a non-existent version.

TurfBook is a sports-venue booking marketplace (like Airbnb for sports venues). This is the production-grade Spring Boot backend.

---

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8.0+

---

## MySQL Setup

Run the following SQL to create the database and user:

```sql
CREATE DATABASE IF NOT EXISTS turfbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'turfbook'@'localhost' IDENTIFIED BY 'turfbook_pass';
GRANT ALL PRIVILEGES ON turfbook.* TO 'turfbook'@'localhost';
FLUSH PRIVILEGES;
```

Or, if you prefer to use root (development only):
```sql
CREATE DATABASE IF NOT EXISTS turfbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## Contract-First / OpenAPI Generator

This project follows a **contract-first** approach:

1. `src/main/resources/openapi/api.yaml` is the **source of truth** for all API shapes.
2. At build time, `openapi-generator-maven-plugin 7.6.0` generates:
   - **DTOs** (request/response Java classes) into `target/generated-sources/openapi/src/main/java/com/turfbook/backend/dto/`
   - **API interfaces** (Spring MVC `@RequestMapping` interfaces) into `target/generated-sources/openapi/src/main/java/com/turfbook/backend/api/`
3. Controllers in `com.turfbook.backend.controller` **implement** the generated API interfaces.
4. Never edit generated files — edit `api.yaml` instead and re-run `mvn generate-sources`.

---

## Maven Commands

```bash
# Generate OpenAPI sources only
mvn generate-sources

# Full build (compile + test)
mvn clean install

# Run the application
mvn spring-boot:run

# Run with custom DB credentials
DB_URL=jdbc:mysql://localhost:3306/turfbook DB_USER=turfbook DB_PASS=turfbook_pass mvn spring-boot:run
```

---

## Swagger UI

Once running, open: **http://localhost:8080/swagger-ui.html**

API docs JSON: http://localhost:8080/api-docs

---

## Environment Variables

| Variable      | Default                                                    | Description              |
|---------------|------------------------------------------------------------|--------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/turfbook?...`                 | JDBC connection URL      |
| `DB_USER`     | `root`                                                     | MySQL username           |
| `DB_PASS`     | `root`                                                     | MySQL password           |
| `JWT_SECRET`  | `turfbook-super-secret-key-change-in-production-...`       | JWT signing secret       |

**Production**: Always override `JWT_SECRET` with a cryptographically random 256-bit key.

```bash
# Generate a secure secret
openssl rand -base64 32
```

---

## Project Structure

```
turfbook-backend/
├── src/main/
│   ├── java/com/turfbook/backend/
│   │   ├── config/         # Spring Security, CORS, OpenAPI configs
│   │   ├── controller/     # REST controllers (implement generated API interfaces)
│   │   ├── entity/         # JPA entities + converters
│   │   ├── exception/      # Global exception handler + custom exceptions
│   │   ├── mapper/         # MapStruct mappers (entity ↔ DTO)
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── security/       # JWT provider, filter, UserDetailsService
│   │   └── service/        # Service interfaces + implementations
│   └── resources/
│       ├── application.yml
│       ├── db/migration/   # Flyway SQL migrations
│       └── openapi/api.yaml
└── target/generated-sources/openapi/  # Generated at build time — do not edit
```

---

## Tech Stack

| Technology                    | Version  |
|-------------------------------|----------|
| Java                          | 21       |
| Spring Boot                   | 3.5.1    |
| Spring Security               | 6.x      |
| Spring Data JPA / Hibernate   | 6.x      |
| MySQL Connector               | 8.x      |
| Flyway                        | 10.x     |
| openapi-generator-maven-plugin| 7.6.0    |
| springdoc-openapi              | 2.8.3    |
| MapStruct                     | 1.6.3    |
| Lombok                        | 1.18.36  |
| jjwt                          | 0.12.6   |
