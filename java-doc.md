What was created — 87 files
Version note
Spring Boot 3.5.1 was used (not 4.x — Spring Boot 4 / Spring Framework 7 has not been released yet). This is stated clearly in the README.

Directory structure

turfbook-backend/
├── pom.xml                              ← Full Maven build
├── README.md                            ← Setup guide
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.yml          ← MySQL + JWT + Flyway config
    │   │   ├── openapi/api.yaml         ← CONTRACT (source of truth)
    │   │   └── db/migration/V1__init_schema.sql
    │   └── java/com/turfbook/backend/
    │       ├── config/                  ← CORS, OpenAPI, Security
    │       ├── security/                ← JWT provider, filter, UserDetails
    │       ├── entity/                  ← 13 JPA entities + JsonListConverter
    │       ├── repository/              ← 12 Spring Data repos
    │       ├── service/ + impl/         ← 11 interfaces + impls
    │       ├── controller/              ← 13 REST controllers
    │       ├── mapper/                  ← 10 MapStruct mappers
    │       └── exception/               ← Global handler + error DTO
    └── test/                            ← Context load + BookingService unit tests
Contract-first flow
src/main/resources/openapi/api.yaml defines all 50+ endpoints and 30+ schemas
mvn generate-sources runs openapi-generator-maven-plugin 7.6.0 → generates DTOs into target/generated-sources/openapi/com/turfbook/backend/dto/ and API interfaces into .../api/
build-helper-maven-plugin adds the generated directory to the compile path
Controllers implements BookingsApi, implements VenuesApi, etc. — zero boilerplate routing
Fully implemented vertical slice — Booking
BookingServiceImpl.java — slot AVAILABLE check → coupon validation → commission=10% → slot locked → notifications to player + owner → cancel with 24h/12h/0h refund tiers
BookingController.java — implements generated BookingsApi, @PreAuthorize role guards, no business logic
Run commands

# 1. Create MySQL database
mysql -u root -p -e "CREATE DATABASE turfbook CHARACTER SET utf8mb4;"

# 2. Generate DTOs + API interfaces
mvn generate-sources

# 3. Build everything
mvn clean install

# 4. Run
mvn spring-boot:run
# or: java -jar target/turfbook-backend-1.0.0-SNAPSHOT.jar
Swagger UI: http://localhost:8080/swagger-ui.html

Environment variables for production:

Var	Example
DB_URL	jdbc:mysql://prod-host:3306/turfbook
DB_USER	turfbook_user
DB_PASS	••••••
JWT_SECRET	256-bit base64 string
