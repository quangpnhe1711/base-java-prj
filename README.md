<div align="center">

# base-java-prj

**Company Java / Spring Boot project baseline**

Start a new backend service with the conventions already in place — error contract, i18n,
auditing, soft delete, optimistic locking, security, tests and CI — and none of the
business logic of the project it was extracted from.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-internal-lightgrey)](#license)

</div>

---

## Table of contents

1. [What this is](#1-what-this-is)
2. [Quick start](#2-quick-start)
3. [Architecture](#3-architecture)
4. [Module reference](#4-module-reference)
5. [Package structure](#5-package-structure)
6. [Conventions](#6-conventions)
7. [Creating a new service](#7-creating-a-new-service)
8. [Adding a feature](#8-adding-a-feature)
9. [Configuration](#9-configuration)
10. [Database and migrations](#10-database-and-migrations)
11. [Security](#11-security)
12. [Error handling and i18n](#12-error-handling-and-i18n)
13. [Optional modules](#13-optional-modules)
14. [Testing](#14-testing)
15. [Build, run and CI](#15-build-run-and-ci)
16. [Decisions and rationale](#16-decisions-and-rationale)
17. [Troubleshooting](#17-troubleshooting)

---

## 1. What this is

A **runnable baseline**, not a code generator and not a framework.

You get five Maven modules. Four are shared libraries that self-configure when they are on
the classpath. The fifth, `service-template`, is a complete working service with one CRUD
resource, three kinds of test, its own migration and its own YAML. You copy that module,
rename it, delete the sample resource, and start writing your own.

### What is included

| | |
|---|---|
| ✅ | Base entity with UUID key, audit columns and soft delete |
| ✅ | Repository base with soft-delete-aware lookups |
| ✅ | One error contract for every failing endpoint, in every status code |
| ✅ | Error catalogue with stable `ERRxxx` codes, English and Vietnamese |
| ✅ | Locale from `Accept-Language`, no `lang` parameter, no `Locale` arguments |
| ✅ | OAuth2 resource server with Keycloak realm-role mapping — **and a switch to run without one** |
| ✅ | Optimistic locking on `updatedAt`, exposed as part of the API contract |
| ✅ | Paged search with a native query and a projection |
| ✅ | MapStruct mapping, checked at compile time |
| ✅ | Flyway migrations, Checkstyle enforced at build time |
| ✅ | Unit, web-slice and Testcontainers integration tests |
| ✅ | OpenAPI / Swagger UI |
| ✅ | `docker compose` stack: PostgreSQL by default, Keycloak / MinIO / observability behind profiles |
| ✅ | GitLab CI for develop / staging / production, Jib images, no Dockerfile |
| ✅ | Optional S3 storage module (AWS S3 and MinIO through one code path) |
| ✅ | Optional Excel import/export module, annotation-driven |

### What is deliberately absent

| | |
|---|---|
| ❌ | Any business domain |
| ❌ | An API gateway module — [see below](#api-gateway) |
| ❌ | A mandatory identity provider — Keycloak is optional infrastructure |
| ❌ | Message broker wiring — add it when a service actually publishes something |
| ❌ | A local-filesystem storage backend — MinIO speaks S3, so there is nothing to abstract |
| ❌ | Interfaces with one implementation, factories for one product, config for constants |

---

## 2. Quick start

### Prerequisites

- JDK 17 or later
- Maven 3.9+
- Docker with Compose v2

### Run it

```bash
git clone https://github.com/quangpnhe1711/base-java-prj.git
cd base-java-prj

# 1. Start PostgreSQL — the only thing the service cannot run without
docker compose up -d

# 2. Build everything and run the unit tests
mvn clean install

# 3. Run the reference service
mvn -pl service-template spring-boot:run -Dspring-boot.run.profiles=local
```

That is the whole loop. **No identity provider is required**: the `local` profile ships
with `app.security.enabled: false`, so the service starts against a database alone and warns
loudly that it is open.

### Verify

```bash
curl http://localhost:8080/actuator/health
#  {"status":"UP","groups":["liveness","readiness"]}

# Create, read back, update using the returned lock token
curl -s -X POST http://localhost:8080/api/v1/samples \
  -H 'Content-Type: application/json' \
  -d '{"code":"ABC_1","name":"Alpha","status":"ACTIVE","effectiveDate":"2026-01-15"}'
#  {"id":"…","code":"ABC_1","name":"Alpha","status":"ACTIVE","updatedAt":"2026-01-15T10:00:00+07:00"}

curl -s 'http://localhost:8080/api/v1/samples?page=1&pageSize=10&orderBy=code'

# Error contract, in Vietnamese
curl -s -X POST http://localhost:8080/api/v1/samples \
  -H 'Content-Type: application/json' -H 'Accept-Language: vi' \
  -d '{"code":"ABC_1","name":"Dup","status":"ACTIVE"}'
#  409 {"code":"ERR002","message":"Mã bản ghi mẫu đã tồn tại.", …}
```

API documentation: <http://localhost:8080/swagger-ui.html>

### Optional infrastructure

Everything except PostgreSQL is opt-in, so the default startup stays fast.

```bash
docker compose up -d                          # PostgreSQL only
docker compose --profile auth up -d           # + Keycloak
docker compose --profile storage up -d        # + MinIO
docker compose --profile observability up -d  # + Prometheus, Grafana, Loki, Zipkin
docker compose --profile all up -d            # everything
```

To work on anything authentication-related, start Keycloak and switch security on:

```bash
docker compose --profile auth up -d

mvn -pl service-template spring-boot:run \
    -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.arguments="--app.security.enabled=true --server.port=8081"

TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/base/protocol/openid-connect/token \
  -d client_id=base-client -d grant_type=password \
  -d username=editor -d password=editor | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" \
     'http://localhost:8081/api/v1/samples?page=1&pageSize=20'
```

> **Note** — Keycloak also listens on `8080`, which is why the service moves to `8081` above.

### Local endpoints

| Service | URL | Credentials | Profile |
|---|---|---|---|
| Reference service | http://localhost:8080 | none, or bearer token | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — | — |
| PostgreSQL | `localhost:5432` | `postgres` / `postgres` | default |
| Keycloak | http://localhost:8080 | `admin` / `admin` | `auth` |
| MinIO console | http://localhost:9001 | `minioadmin` / `minioadmin` | `storage` |
| Grafana | http://localhost:3001 | `admin` / `admin` | `observability` |
| Prometheus | http://localhost:9090 | — | `observability` |
| Zipkin | http://localhost:9411 | — | `observability` |
| Loki | http://localhost:3100 | — | `observability` |

The imported realm provides `editor` / `editor` (roles `SAMPLE_VIEWER`, `SAMPLE_EDITOR`) and
`viewer` / `viewer` (role `SAMPLE_VIEWER`).

---

## 3. Architecture

Classic layered architecture. It is not fashionable and it is the right default: everybody
on the team already knows where to look, and a service this size gains nothing from a
hexagonal rewrite.

```
                    HTTP
                      │
        ┌─────────────▼──────────────┐
        │        Controller          │   bind · validate · delegate
        │   thin, no business rules  │
        └─────────────┬──────────────┘
                      │  DTO
        ┌─────────────▼──────────────┐
        │      Service + Impl        │   rules · transactions · orchestration
        │  interface / impl split    │
        └──────┬──────────────┬──────┘
               │              │
      Entity   │              │   Projection
        ┌──────▼──────┐  ┌────▼─────────────┐
        │ Repository  │  │  Native SQL in    │
        │ Spring Data │  │  a constant class │
        └──────┬──────┘  └────┬─────────────┘
               └───────┬───────┘
                       │
                 ┌─────▼─────┐
                 │ PostgreSQL│   schema owned by Flyway
                 └───────────┘
```

Cross-cutting concerns never live in the layers. They arrive from the shared modules:

```
┌───────────────────────────────────────────────────────────────┐
│  common-core        error contract · i18n · BaseEntity ·      │
│                     BaseRepository · validation · locking     │
├───────────────────────────────────────────────────────────────┤
│  common-security    resource server · role mapping ·          │
│                     current user · JPA auditing source        │
├───────────────────────────────────────────────────────────────┤
│  common-storage ○   S3 / MinIO object storage                 │
│  common-excel   ○   annotation-driven import and export       │
└───────────────────────────────────────────────────────────────┘
                        ○ = optional
```

Every shared module is a Spring Boot **auto-configuration**. Put it on the classpath and it
wires itself; a service only component-scans its own package. Every bean is
`@ConditionalOnMissingBean`, so declaring your own of the same type replaces it.

---

## 4. Module reference

```
base-java-prj
├── common-core          shared infrastructure, no domain, no security
├── common-security      OAuth2 resource server, roles, current user, auditing
├── common-storage    ○  object storage over the S3 API
├── common-excel      ○  Excel import and export
└── service-template     runnable reference service — copy this one
```

### `common-core`

| Component | Purpose |
|---|---|
| `BaseEntity` | UUID key, `createdUserId`/`createdAt`/`updatedUserId`/`updatedAt`, `deleteFlag` |
| `BaseRepository<T, I>` | `findByIdAndDeleteFlagFalse`, `findAllByDeleteFlagFalse`, `existsByIdAndDeleteFlagFalse` |
| `ApiError` | The single error body of the whole API |
| `GlobalExceptionHandler` | Maps every exception onto `ApiError`; never leaks internal text |
| `BusinessException` + subclasses | `ResourceNotFoundException`, `DuplicateResourceException`, `StaleDataException` |
| `ErrorCode` | Stable `ERRxxx` catalogue |
| `MessageKey` | Interface each service implements with its own domain labels |
| `MessageUtil` | Facade over `MessageSource`, resolves the locale itself |
| `OptimisticLockSupport` | `loadForUpdate`, `softDelete` — lost-update protection |
| `BasePageRequest` / `BasePageResponse` | Paging contract, one-based on the wire |
| `DeleteBaseRequest` | Delete payload carrying the lock token |
| `ValidatorWrapper` | Bean Validation outside the MVC boundary |
| `@RequiredIf` | Class-level conditional presence rule |
| `SqlUtils` / `StringUtil` / `DateTimeUtil` | Small, boring helpers |

### `common-security`

| Component | Purpose |
|---|---|
| `SecurityConfig` | The one filter chain: stateless, bearer-only, method security on |
| `SecurityProperties` | `app.security.*` — public paths, client id, CORS |
| `JwtRoleConverter` | `realm_access.roles` and `resource_access.<client>.roles` → `ROLE_*` |
| `SecurityUtils` | Current user id / username / email from the **verified** token |
| `AuditorAwareImpl` | Feeds the audit columns |
| `ApiErrorSecurityEntryPoint` | `ApiError` body for filter-level 401 and 403 |
| `SecurityExceptionHandler` | `ApiError` body for `@PreAuthorize` rejections |

### `service-template`

A complete `Sample` resource: entity, DTOs, mapper, repository with a native paged search,
service, controller, migration, messages and three levels of test. Everything in it exists
to demonstrate one convention. Delete it once you have copied the shape.

---

## 5. Package structure

```
com.luvina.base.<service>
├── config/          Spring configuration owned by this service
├── constant/        MessageKey enum, service constants
├── controller/      REST endpoints, one per resource
├── dto/
│   ├── request/     inbound payloads, Bean Validation lives here
│   └── response/    outbound payloads, never an entity
├── entity/          JPA entities, all extending BaseEntity
├── enums/           domain enums, persisted by name
├── mapper/          MapStruct interfaces
├── projection/      read models for native queries
├── repository/
│   └── sql/         native SQL as text-block constants
└── service/
    └── impl/        implementations
```

```
src/main/resources
├── application.yml            base config, env-var driven
├── application-local.yml      developer machine
├── application-docker.yml     container deployment
├── messages.properties        domain labels (English)
├── messages_vi.properties     domain labels (Vietnamese)
├── logback-spring.xml         console only, traceId in the pattern
└── db/migration/              V{n}__{description}.sql
```

### Naming

| Kind | Pattern | Example |
|---|---|---|
| Controller | `<Resource>Controller` | `SampleController` |
| Service | `<Resource>Service` + `<Resource>ServiceImpl` | `SampleService` |
| Repository | `<Entity>Repository` | `SampleRepository` |
| SQL holder | `<Entity>Sql` | `SampleSql` |
| Projection | `<Entity><View>Projection` | `SampleListProjection` |
| Request DTO | `<Entity><Action>Request` | `SampleCreateRequest` |
| Response DTO | `<Entity>Response`, `<Entity>PageResponse` | `SampleResponse` |
| Enum | `<Concept>` or `<Concept>Status` | `SampleStatus` |
| Table | `t_` transactional, `m_` master | `t_samples`, `m_grades` |
| Column | `snake_case`, mapped explicitly | `effective_date` |
| Endpoint | `/api/v1/<plural-kebab>` | `/api/v1/samples` |

---

## 6. Conventions

### Soft delete everywhere

No row is ever physically deleted. `BaseEntity.deleteFlag` marks it gone; every read goes
through a `...DeleteFlagFalse` method.

```java
// ✅ soft-delete aware
Optional<Sample> found = sampleRepository.findByIdAndDeleteFlagFalse(id);

// ❌ returns logically deleted rows
Optional<Sample> found = sampleRepository.findById(id);
```

Uniqueness must therefore be a **partial index**, or a deleted row blocks reuse of its code
forever:

```sql
CREATE UNIQUE INDEX ux_samples_code_active
    ON t_samples (code)
    WHERE delete_flag = FALSE;
```

### Optimistic locking on `updatedAt`

`updatedAt` is part of the public contract. A client reads it, sends it back on the next
write, and the service refuses the write if the stored value has moved on.

```
GET  /api/v1/samples/{id}   →  { ..., "updatedAt": "2026-01-01T10:00:00+07:00" }
PUT  /api/v1/samples/{id}      { ..., "updatedAt": "2026-01-01T10:00:00+07:00" }
                            →  409 ERR029 if somebody else changed it meanwhile
```

```java
Sample sample = lockSupport.loadForUpdate(
        sampleRepository, id, request.getUpdatedAt(), TemplateMessageKey.SAMPLE);
sampleMapper.update(request, sample);
// no save() — the entity is managed, the change is flushed at commit
```

### Controllers stay thin

Bind, validate, delegate, return. No rules, no repository access, no `try`/`catch`.

```java
@PostMapping
@PreAuthorize("hasRole('SAMPLE_EDITOR')")
public ResponseEntity<SampleResponse> create(@Valid @RequestBody SampleCreateRequest request) {
    SampleResponse created = sampleService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/samples/" + created.getId())).body(created);
}
```

### Validation is layered

| Rule | Where |
|---|---|
| Shape: required, length, pattern, range | Bean Validation on the request DTO |
| Conditional presence across fields | `@RequiredIf` on the request class |
| Anything needing a query: uniqueness, references, state machines | The service |

### Native SQL lives in a constant class

Complex SQL belongs in a text block where it can be read and reviewed, not squeezed into an
annotation. Parameters are always bound, never concatenated. `LIKE` patterns declare their
escape character, which is what makes `SqlUtils.escapeLike` meaningful.

```java
public static final String SEARCH = """
    SELECT s.id AS id, s.code AS code, s.updated_at AS updatedAt
    FROM t_samples s
    WHERE s.delete_flag = FALSE
      AND (CAST(:code AS TEXT) IS NULL
           OR LOWER(s.code) LIKE LOWER(CONCAT('%', CAST(:code AS TEXT), '%')) ESCAPE '\\')
    """;
```

Sort columns come from an allow-list. The column name ends up inside `ORDER BY`, so
accepting an arbitrary string from the client would be an injection point.

```java
private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
        "code", "code",
        "updatedAt", "updated_at");
```

### Transactions

```java
@Transactional(readOnly = true)   // every read
@Transactional                    // every write
```

### Constructor injection only

`@RequiredArgsConstructor` with `private final` fields. No field injection, no setter
injection: a class whose dependencies are not visible in its constructor is a class you
cannot instantiate in a test.

---

## 7. Creating a new service

```bash
# 1. Copy the template
cp -r service-template order-service
cd order-service

# 2. Rename the artifact
#    pom.xml: <artifactId>order-service</artifactId>

# 3. Rename the package
mv src/main/java/com/luvina/base/template src/main/java/com/luvina/base/order
mv src/test/java/com/luvina/base/template src/test/java/com/luvina/base/order
grep -rl 'com.luvina.base.template' src | xargs sed -i 's/com.luvina.base.template/com.luvina.base.order/g'
mv src/main/java/com/luvina/base/order/TemplateApplication.java \
   src/main/java/com/luvina/base/order/OrderApplication.java
sed -i 's/TemplateApplication/OrderApplication/g' $(grep -rl TemplateApplication src)

# 4. Register the module in the parent pom.xml
#    <module>order-service</module>

# 5. Point the config at your own service
#    application.yml: spring.application.name, server.port
#    application-local.yml: database name

# 6. Delete the sample resource, keep the shape
rm -rf src/main/java/com/luvina/base/order/{entity,dto,mapper,projection,repository,service,controller,enums}/*
rm src/main/resources/db/migration/V1__create_sample_table.sql

# 7. Build
mvn -pl order-service -am clean install
```

Keep these:

- `constant/<Service>MessageKey.java` — replace the constants with your own labels
- `config/OpenApiConfig.java`
- `application*.yml`, `messages*.properties`, `logback-spring.xml`
- the three test classes, as the shape for your own

---

## 8. Adding a feature

A full vertical slice, in the order it is normally written.

<details>
<summary><b>1. Migration</b> — <code>db/migration/V7__create_orders_table.sql</code></summary>

```sql
CREATE TABLE t_orders (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_no        VARCHAR(32)  NOT NULL,
    customer_name   VARCHAR(255) NOT NULL,
    total_amount    NUMERIC(15,2) NOT NULL,
    status          VARCHAR(16)  NOT NULL,

    created_user_id UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_user_id UUID,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    delete_flag     BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ux_orders_no_active
    ON t_orders (order_no) WHERE delete_flag = FALSE;
```

Rules: versions strictly increasing, never reused, and an applied file is **immutable** —
to change something, add the next version.

</details>

<details>
<summary><b>2. Entity</b></summary>

```java
@Entity
@Table(name = "t_orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status = OrderStatus.DRAFT;
}
```

`EnumType.STRING`, always. Persisting by ordinal means reordering the constants silently
rewrites the meaning of existing rows.

</details>

<details>
<summary><b>3. Message keys</b></summary>

```java
public enum OrderMessageKey implements MessageKey {
    ORDER, ORDER_NO, CUSTOMER_NAME;

    @Override
    public String key() {
        return name();
    }
}
```

```properties
# messages.properties
ORDER=Order
ORDER_NO=Order number
CUSTOMER_NAME=Customer name
```

```properties
# messages_vi.properties
ORDER=Đơn hàng
ORDER_NO=Số đơn hàng
CUSTOMER_NAME=Tên khách hàng
```

</details>

<details>
<summary><b>4. DTOs</b></summary>

```java
@Getter
@Setter
public class OrderCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String orderNo;

    @NotBlank
    @Size(max = 255)
    private String customerName;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalAmount;
}

@Getter
@Setter
public class OrderSearchRequest extends BasePageRequest {
    private String orderNo;
    private OrderStatus status;
}

@Getter
@Setter
public class OrderPageResponse extends BasePageResponse {
    private List<OrderResponse> orderList;
}
```

Always include `updatedAt` in the response DTO, or the client cannot perform a safe update.

</details>

<details>
<summary><b>5. Repository, SQL and projection</b></summary>

```java
public interface OrderListProjection {
    UUID getId();
    String getOrderNo();
    String getStatus();
    Instant getUpdatedAt();     // native queries return Instant
}
```

```java
@Repository
public interface OrderRepository extends BaseRepository<Order, UUID> {

    boolean existsByOrderNoAndDeleteFlagFalse(String orderNo);

    @Query(value = OrderSql.SEARCH, countQuery = OrderSql.SEARCH_COUNT, nativeQuery = true)
    Page<OrderListProjection> search(@Param("orderNo") String orderNo,
                                     @Param("status") String status,
                                     Pageable pageable);
}
```

</details>

<details>
<summary><b>6. Mapper</b></summary>

```java
@Mapper
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedUserId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleteFlag", ignore = true)
    Order toEntity(OrderCreateRequest request);

    OrderResponse toResponse(Order entity);

    List<OrderResponse> toResponseList(List<OrderListProjection> projections);

    default OffsetDateTime toOffsetDateTime(Instant value) {
        return DateTimeUtil.toOffsetDateTime(value);
    }
}
```

The build sets `unmappedTargetPolicy=ERROR`. Every field you intentionally leave alone must
say so, which means adding a field to the entity **forces** a decision in the mapper instead
of producing a silent null in production.

</details>

<details>
<summary><b>7. Service</b></summary>

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OptimisticLockSupport lockSupport;
    private final MessageUtil messageUtil;

    @Override
    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        if (orderRepository.existsByOrderNoAndDeleteFlagFalse(request.getOrderNo())) {
            throw new DuplicateResourceException(
                    messageUtil.format(ErrorCode.ERR002, OrderMessageKey.ORDER_NO));
        }
        return orderMapper.toResponse(orderRepository.save(orderMapper.toEntity(request)));
    }
}
```

</details>

<details>
<summary><b>8. Controller</b></summary>

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<OrderPageResponse> search(@Valid @ModelAttribute OrderSearchRequest request) {
        return ResponseEntity.ok(orderService.search(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORDER_EDITOR')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse created = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.getId())).body(created);
    }
}
```

</details>

<details>
<summary><b>9. Tests</b></summary>

Copy the three template test classes and adapt them:

| File | Level | Covers |
|---|---|---|
| `OrderServiceImplTest` | unit, no Spring | rules, mapping, error paths |
| `OrderControllerTest` | `@WebMvcTest` | routing, binding, validation, error shape |
| `OrderRepositoryIT` | `@DataJpaTest` + Testcontainers | migration, native SQL, indexes |

</details>

---

## 9. Configuration

Three files, one job each.

| File | Role |
|---|---|
| `application.yml` | Base. Environment variables with safe defaults. Never a secret. |
| `application-local.yml` | Developer machine. Points at `docker compose`. |
| `application-docker.yml` | Container. Everything from the environment, because one image is promoted through every environment unchanged. |

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | Active profile |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `base_java_prj` | Database |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | Credentials |
| `DB_POOL_MAX` | `10` | Hikari pool size |
| `SECURITY_ENABLED` | `true` | Enforce token authentication. `false` runs without an identity provider |
| `OIDC_ISSUER_URI` | none | Token issuer. Required when security is enabled |
| `OIDC_CLIENT_ID` | — | Client whose `resource_access` roles are granted |
| `CORS_ALLOWED_ORIGINS` | empty | Comma-separated origins; empty keeps CORS **off** |
| `TRACING_SAMPLE_RATE` | `0.1` | Zipkin sampling |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | Trace collector |
| `API_DOCS_ENABLED` | `true` | Swagger UI and `/v3/api-docs` |
| `LOG_LEVEL` | `INFO` | Level for `com.luvina.base` |
| `UPLOAD_MAX_FILE_SIZE` | `10MB` | Multipart limit |
| `S3_ENDPOINT` | empty | Empty = real AWS; set it for MinIO |
| `S3_REGION` / `S3_BUCKET` | — | Object storage |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | empty | Omit both to use the instance role |

### Rules

- **Secrets never enter a file.** They arrive as environment variables.
- `spring.jpa.hibernate.ddl-auto: none` in **every** environment. Flyway owns the schema.
- `spring.jpa.open-in-view: false`. Boot enables it; keeping a session open for the whole
  request hides lazy-loading mistakes until production.
- `server.error.include-message: never`. Error bodies come from `GlobalExceptionHandler`;
  the container must not add its own.

---

## 10. Database and migrations

### Migration rules

1. `V{n}__{snake_case_description}.sql` in `src/main/resources/db/migration/`
2. Versions strictly increasing, never reused
3. **An applied migration is immutable.** Editing it breaks Flyway's checksum for everyone
   who already ran it. To change something, add the next version.
4. Never `ddl-auto: update`, in any environment

### Every table

```sql
created_user_id UUID,
created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
updated_user_id UUID,
updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
delete_flag     BOOLEAN NOT NULL DEFAULT FALSE
```

`created_user_id` is nullable on purpose: scheduled jobs and migrations write rows with no
authenticated caller, and inventing a placeholder id only pollutes the audit trail.

### Useful queries

```sql
-- What has been applied
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

-- Rows a soft delete is hiding
SELECT count(*) FROM t_samples WHERE delete_flag = TRUE;
```

### Seed data

Master data belongs in a normal migration, not in a bespoke callback mechanism. A seeder
that runs outside `flyway_schema_history` is a seeder nobody can audit.

```sql
-- V2__seed_sample_master_data.sql
INSERT INTO m_something (code, name, priority) VALUES
    ('01', 'First', 1),
    ('02', 'Second', 2)
ON CONFLICT (code) DO NOTHING;
```

---

## 11. Security

### How it works

```
        Bearer token
             │
   ┌─────────▼──────────┐
   │  Resource server   │  signature · issuer · audience · expiry
   │  (Spring Boot)     │  JwtDecoder built from issuer-uri
   └─────────┬──────────┘
             │  verified Jwt
   ┌─────────▼──────────┐
   │  JwtRoleConverter  │  realm_access.roles          → ROLE_*
   │                    │  resource_access.<cid>.roles → ROLE_*
   │                    │  scopes                      → SCOPE_*
   └─────────┬──────────┘
             │
   ┌─────────▼──────────┐        ┌──────────────────────┐
   │  @PreAuthorize     │        │  SecurityUtils       │
   │  per endpoint      │        │  current user id     │
   └────────────────────┘        └──────────┬───────────┘
                                            │
                                 ┌──────────▼───────────┐
                                 │  AuditorAwareImpl    │
                                 │  audit columns       │
                                 └──────────────────────┘
```

### Keycloak is optional

An identity provider is a dependency of *authentication*, not of the service. Set
`app.security.enabled: false` and the chain permits every request, no `JwtDecoder` is built,
method security is not enabled, and the service starts against a database alone — which is
exactly what the `local` profile does.

That mode logs a warning **on every startup**, so an unsecured service cannot go unnoticed:

```
WARN  c.l.b.security.SecurityConfig - app.security.enabled=false - every endpoint is OPEN
      and no token is verified. Intended for local development only.
```

Deployed environments leave it `true`. Nothing else in the baseline is Keycloak-specific
except `JwtRoleConverter`; another identity provider only needs its own converter bean.

### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI}

app:
  security:
    enabled: true                          # false = run with no identity provider
    resource-client-id: ${OIDC_CLIENT_ID:}
    public-paths:
      - /actuator/health
      - /actuator/health/**
      - /v3/api-docs/**
      - /swagger-ui/**
    cors:
      allowed-origins:
        - https://app.example.com
```

### Rules

> [!IMPORTANT]
> **Never declare a second `SecurityFilterChain`.** With two unordered chains the effective
> policy is whichever one Spring registers first. That is how an API ends up silently open —
> and it is exactly the defect this baseline was extracted away from.

> [!IMPORTANT]
> **Never read claims from the `Authorization` header yourself.** An unverified token is
> attacker-controlled input; using its `sub` for audit columns lets anyone write rows under
> any identity. Use `SecurityUtils`, which reads the token Spring already verified.

- Everything is authenticated unless its path is in `app.security.public-paths`
- Stateless, no session, no login form — so CSRF protection does not apply
- CORS stays **off** until an origin is configured: a misconfigured environment fails closed
- `@PreAuthorize` on the endpoint, next to the code it protects

### Getting a token locally

```bash
curl -s -X POST http://localhost:8080/realms/base/protocol/openid-connect/token \
  -d client_id=base-client -d grant_type=password \
  -d username=editor -d password=editor | jq -r .access_token
```

---

## 12. Error handling and i18n

### One error body, always

```json
{
  "timestamp": "2026-01-01T10:00:00+07:00",
  "status": 400,
  "code": "ERR014",
  "message": "Some fields are invalid. Please review the highlighted items.",
  "path": "/api/v1/samples",
  "fieldErrors": [
    { "field": "code", "message": "code accepts uppercase letters, digits and underscore only" },
    { "field": "name", "message": "must not be blank" }
  ]
}
```

Same shape for 400, 401, 403, 404, 409, 413 and 500. A client never has to branch on the
status code to know how to parse a failure.

### Mapping

| Exception | Status | Code |
|---|---|---|
| `BusinessException` | as declared | as declared |
| `ResourceNotFoundException` | 404 | `ERR003` |
| `DuplicateResourceException` | 409 | `ERR002` / `ERR027` |
| `StaleDataException` | 409 | `ERR029` |
| `MethodArgumentNotValidException` | 400 | `ERR014` + `fieldErrors` |
| `ConstraintViolationException` | 400 | `ERR014` + `fieldErrors` |
| `MethodArgumentTypeMismatchException` | 400 | `ERR017` |
| `HttpMessageNotReadableException` | 400 | `ERR017` |
| `MaxUploadSizeExceededException` | 413 | `ERR023` |
| `DataIntegrityViolationException` | 409 | `ERR002` |
| `AccessDeniedException` | 403 | `ERR040` |
| `AuthenticationException` | 401 | `ERR041` |
| anything else | 500 | `ERR015` — logged with stack trace, generic message out |

### The catalogue

`ERRxxx` codes live in `common-core` (`ErrorCode` + `messages/core-errors*.properties`) and
are **stable forever**: clients and QA scripts match on them. Domain *labels* live per
service.

```java
// "Sample code already exists."  /  "Mã bản ghi mẫu đã tồn tại."
messageUtil.format(ErrorCode.ERR002, TemplateMessageKey.SAMPLE_CODE)
```

Two bundles are merged, so a service ships its own `messages.properties` without shadowing
the shared catalogue:

```
classpath:messages/core-errors   ← common-core, ERRxxx
classpath:messages               ← this service, domain labels
```

### Locale

From `Accept-Language`. Nothing else.

```
Accept-Language: vi   →  Vietnamese
Accept-Language: en   →  English
(absent)              →  English
```

> No `?lang=` parameter, and **no `Locale` argument threaded through service signatures**.
> The locale is ambient in `LocaleContextHolder` and `MessageUtil` reads it there.

---

## 13. Optional modules

### `common-storage`

One backend, the S3 API. MinIO speaks it in development, AWS S3 in production, so there is
one code path and no `storage.type` switch to get wrong.

```xml
<dependency>
  <groupId>com.luvina.base</groupId>
  <artifactId>common-storage</artifactId>
  <version>${project.version}</version>
</dependency>
```

```yaml
app:
  storage:
    endpoint: http://localhost:9000   # empty = real AWS
    region: us-east-1
    bucket: base-local
    access-key: ${S3_ACCESS_KEY:}     # omit both to use the instance role
    secret-key: ${S3_SECRET_KEY:}
    path-style-access: true           # required by MinIO
    presigned-url-ttl: 1h
```

```java
String key = storageService.upload(file, "invoices");   // → invoices/<uuid>.pdf
URL url   = storageService.presignedDownloadUrl(key);
boolean ok = storageService.exists(key);
storageService.delete(key);
```

Keys are always `folder/uuid.ext`. A caller-supplied file name is never used as a key: it
collides, and it lets a caller steer where the object lands.

Activates only when `app.storage.bucket` is set.

### `common-excel`

Annotation-driven, reusable across projects, and it never stops at the first bad row.

```xml
<dependency>
  <groupId>com.luvina.base</groupId>
  <artifactId>common-excel</artifactId>
  <version>${project.version}</version>
</dependency>
```

```java
public class PersonRow {

    @ExcelColumn(header = "Full name", order = 1, required = true)
    @NotBlank
    private String name;

    @ExcelColumn(header = "Age", order = 2)
    @Min(0)
    private Integer age;

    @ExcelColumn(header = "Start date", order = 3, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
}
```

```java
ExcelParseResult<PersonRow> result = excelReader.read(file.getInputStream(), PersonRow.class);

if (!result.isClean()) {
    byte[] report = excelWriter.writeErrorReport(result.errors(), headers);
    return download(report);           // every problem, in one round trip
}
personService.importAll(result.values());
```

```java
byte[] xlsx = excelWriter.write(people, PersonRow.class, "People");
```

| Behaviour | Why |
|---|---|
| Columns matched by **header text**, not position | Reordering the template does not break the import |
| Conversion and validation errors **collected** with the row number | The user fixes everything at once, not one row per upload |
| Bean Validation enforced during import | One set of rules covers the API and the spreadsheet |
| Missing **required** header → whole-file failure | That is the wrong template, not a bad row |
| Blank rows skipped | Trailing blanks are not user errors |
| Violations reported against the **column header** | The user sees "Age", not `person.age` |
| Row cap of 50 000 | A declared last-row far beyond the data is a way to exhaust memory |

Supported types: `String`, `Integer`, `Long`, `Double`, `BigDecimal`, `Boolean`,
`LocalDate`, `LocalDateTime`, `UUID`, enums.

### API gateway

Not included, by design: a single service does not need one.

Add it when you have **more than one** service to route between, or need edge concerns
(rate limiting, request aggregation, one TLS termination point) that do not belong in a
service. It is a separate module:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

Then in the parent `pom.xml` re-add the Spring Cloud BOM, and remember that a gateway is
**reactive** — its security configuration is `SecurityWebFilterChain`, not
`SecurityFilterChain`, so `common-security` does not apply to it. Keep CORS origins in
configuration; never hardcode a hostname.

---

## 14. Testing

Three levels, each with a job.

| Level | Annotation | Speed | Covers | Runs in `mvn test` |
|---|---|---|---|---|
| Unit | none | ms | rules, mapping, error paths | ✅ |
| Web slice | `@WebMvcTest` | ~1 s | routing, binding, validation, error shape | ✅ |
| Integration | `@DataJpaTest` + Testcontainers | ~30 s | migration, native SQL, indexes | ❌ `-Pit` |

```bash
mvn test                # unit + web slice, no Docker needed
mvn verify -Pit         # everything, needs a Docker daemon
```

`*IT.java` is excluded from `mvn test` on purpose, so a machine without Docker can still
build.

### Level 1 — unit

The real MapStruct mapper is used rather than a mock: it is generated code, so exercising it
also checks the mapping annotations. A mocked mapper passes even with a field left unmapped.

```java
@ExtendWith(MockitoExtension.class)
class SampleServiceImplTest {

    @Mock private SampleRepository sampleRepository;
    @Mock private OptimisticLockSupport lockSupport;
    @Mock private MessageUtil messageUtil;

    private SampleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SampleServiceImpl(
                sampleRepository, Mappers.getMapper(SampleMapper.class), lockSupport, messageUtil);
    }
}
```

### Level 2 — web slice

```java
@WebMvcTest(controllers = SampleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, MessageSourceConfig.class, MessageUtil.class})
class SampleControllerTest {
    @MockitoBean private SampleService sampleService;
}
```

Security filters are off. Left on, every request would be answered by the *framework
default* chain, which is not the chain this service runs — the assertions would describe
something that does not exist in production. Authorisation belongs in an integration test
that loads the real chain.

### Level 3 — integration

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class SampleRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

A real database is not optional here. The native search relies on PostgreSQL casts and a
partial unique index; an in-memory database would either reject the SQL or accept writes
that production rejects. The schema is built by the same Flyway migrations that run in
production, so the migration itself is under test.

---

## 15. Build, run and CI

### Commands

```bash
mvn clean install                        # build everything, unit tests, Checkstyle
mvn test                                 # unit + web slice
mvn verify -Pit                          # add integration tests (needs Docker)
mvn checkstyle:check                      # style only
mvn -pl service-template spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl service-template -am -DskipTests compile jib:dockerBuild   # local image
```

### Checkstyle

Runs in the `validate` phase and **fails the build**. Correctness checks are `error`;
documentation and taste checks are `warning` so they inform without blocking a merge. Main
sources only.

> The configuration this was extracted from set `severity=warning` globally while relying on
> `failOnViolation`, which meant nothing ever failed. Here `violationSeverity=error` closes
> that gap.

### Container images

Jib, no Dockerfile and no Docker daemon in CI.

| Profile | Base image |
|---|---|
| default | `eclipse-temurin:17-jre` |
| `-Pdevelop` | internal registry mirror |
| `-Pproduction` | `eclipse-temurin:17-jre` |

### GitLab CI

```
.gitlab-ci.yml            branch → pipeline
└── .gitlab/ci/
    ├── .base.yml         shared jobs: verify · integration-test · sonar · build-image
    ├── develop.yml       push → verify, analyse, build, deploy
    ├── staging.yml       push → verify, analyse, build, manual deploy
    └── production.yml    tag  → verify, analyse, build, manual deploy, blocking gate
```

| Branch | Image tag | Deploy | Quality gate |
|---|---|---|---|
| `develop` | `develop-<sha>` | automatic | advisory |
| `stg` | `stg-<sha>` | manual | advisory |
| `main` / tag | `<tag>` or `main-<sha>` | manual | **blocking** |

Required CI variables: `SONAR_HOST_URL`, `SONAR_TOKEN`, `SONAR_PROJECT_KEY`,
`DEPLOY_SSH_KEY`, `DEPLOY_USER`, `DEPLOY_HOST`, `DEPLOY_PATH`.

### Observability

| Concern | Endpoint |
|---|---|
| Liveness / readiness | `/actuator/health/liveness`, `/actuator/health/readiness` |
| Metrics | `/actuator/prometheus` |
| Traces | Zipkin, `traceId` and `spanId` are in every log line |
| Logs | Console only. Containers write to stdout and the platform ships it; writing files inside a container is a way to lose logs. |

---

## 16. Decisions and rationale

Where this baseline departs from the source project, and why.

| Decision | Rationale |
|---|---|
| **MapStruct** instead of ModelMapper | Compile-time and explicit. The source called `modelMapper.typeMap(...)` inside an update method, mutating global configuration at runtime on every request. |
| **Spring Boot 3.5** instead of 3.2.1 | 3.2.x is out of support. A baseline for new projects must start on a version that still gets security patches. |
| **One error contract** | The source returned `{errors:[…]}` or `{message:…}` depending on the exception, and one constructor split a message on commas — so any message containing a comma arrived fragmented. |
| **`Accept-Language`**, no `lang` parameter | Every endpoint took `?lang=`, and every service method took a `Locale`, but only one Vietnamese bundle existed. The parameter did nothing. |
| **Verified JWT only** | `SecurityUtils` parsed the raw `Authorization` header with `JWTParser.parse()` — no signature check — and fed `sub` into the audit columns. |
| **One filter chain** | A config class declared a second `SecurityFilterChain` with `anyRequest().permitAll()`. With two unordered chains, the effective policy was undefined. |
| **`@RequiredIf` rewritten as class-level** | The original was field-level and tried `context.unwrap(...).unwrap(Object.class)` to reach the bean. That throws, the catch returns `true`, and the validator never validated anything. |
| **No hardcoded auditor** | Soft delete stamped `updated_user_id` with a literal `11111111-2222-…` UUID, making the audit trail useless. |
| **Checkstyle actually fails** | `severity=warning` plus `failOnViolation=true` meant no violation could ever fail the build. |
| **Tests actually run** | `surefire.skipTests=true` was pinned in the POM. |
| **No local-filesystem storage** | MinIO speaks S3. Two backends meant two code paths, a `storage.type` switch, and `D:/uploads` defaults leaking into production config. |
| **No `spring-boot-starter-amqp`** | Declared, never imported by a single line of code. |
| **No `spring-boot-starter-webflux`** | Pulled in for a `WebClient.Builder` bean that nothing used, mixing a reactive stack into an MVC application. |
| **No `poi-ooxml-schemas:4.1.2`** | A dead artifact conflicting with POI 5.x. |
| **Seed data as plain migrations** | The source documented a `FlywaySeederCallback` at length. The class does not exist in the code. |
| **`open-in-view: false`** | The Boot default keeps a session open for the whole request, hiding lazy-loading mistakes until production. |
| **No API gateway module** | One service does not need one. Documented for when a second appears. |
| **Keycloak optional** | An identity provider is a dependency of authentication, not of the service. Requiring one to start a service means nobody can run it on a laptop without the whole stack up. |

---

## 17. Troubleshooting

<details>
<summary><b>Port 8080 already in use</b></summary>

Keycloak also uses `8080`. Run the service elsewhere:

```bash
mvn -pl service-template spring-boot:run \
    -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.arguments="--server.port=8081"
```
</details>

<details>
<summary><b><code>database "base_java_prj" does not exist</code></b></summary>

Another PostgreSQL on the host owns port `5432`. Check who:

```bash
docker ps --format '{{.Names}} {{.Ports}}' | grep 5432
```

Either stop it, or remap this stack's port in `docker-compose.yml` and update
`application-local.yml`.
</details>

<details>
<summary><b>Application fails to start: cannot reach the issuer</b></summary>

Spring Boot builds the `JwtDecoder` from `issuer-uri` **eagerly** — it fetches the OIDC
metadata at startup. Either turn security off:

```bash
mvn -pl service-template spring-boot:run -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.arguments=--app.security.enabled=false
```

or bring Keycloak up with the realm imported:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  http://localhost:8080/realms/base/.well-known/openid-configuration   # expect 200
```
</details>

<details>
<summary><b>Realm not imported</b></summary>

`--import-realm` only reads `/opt/keycloak/data/import` on a **fresh** Keycloak database.
Reset it, or import through the admin API:

```bash
TOKEN=$(curl -s -d client_id=admin-cli -d username=admin -d password=admin \
  -d grant_type=password \
  http://localhost:8080/realms/master/protocol/openid-connect/token | jq -r .access_token)

curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data-binary @docker/keycloak/realms/base-realm.json \
  http://localhost:8080/admin/realms
```
</details>

<details>
<summary><b>MapStruct: <code>Unmapped target property</code></b></summary>

Intentional. `unmappedTargetPolicy=ERROR` forces a decision. Either map the field or say
explicitly that you are not:

```java
@Mapping(target = "someField", ignore = true)
```
</details>

<details>
<summary><b>Testcontainers cannot find Docker</b></summary>

The daemon must be running. In CI, use the `docker:dind` service and set
`DOCKER_HOST` / `TESTCONTAINERS_HOST_OVERRIDE` — see `.gitlab/ci/.base.yml`.
</details>

<details>
<summary><b>Checkstyle fails the build</b></summary>

```bash
mvn checkstyle:check          # full report
```

Common causes: a star import, an unused import, a line over 120 characters, a tab
character. Documentation warnings never fail the build.
</details>

<details>
<summary><b>Flyway: checksum mismatch</b></summary>

An applied migration was edited. Revert the file and add a new version instead. Never
`flyway:repair` on shared or production databases without agreement.
</details>

<details>
<summary><b>409 <code>ERR029</code> on every update</b></summary>

The client is not sending back the `updatedAt` it received, or is sending a stale one.
Re-read the record and use the fresh token.
</details>

---

## License

Internal. Copy it into a new repository and make it yours.
