# Enterprise Spring Boot 3 + Advanced MongoDB Architecture Reference

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Data MongoDB](https://img.shields.io/badge/Spring%20Data%20MongoDB-4.3+-forestgreen.svg)](https://spring.io/projects/spring-data-mongodb)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0+-green.svg)](https://www.mongodb.com/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-1.20.2-orange.svg)](https://www.testcontainers.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

A production-grade, enterprise-scale reference architecture demonstrating advanced capabilities of **Spring Data MongoDB** on **Java 17/21 LTS** and **Spring Boot 3.3.x**. 

This repository provides production patterns for distributed multi-document ACID transactions, optimistic concurrency control with automated retries, polymorphic document hierarchies, high-precision BSON arithmetic, multi-stage aggregation pipelines with cross-collection lookups, programmatic index lifecycle management, thread-pooled real-time change stream listeners, and comprehensive Testcontainers integration testing.

---

## 📑 Table of Contents
- [1. Architectural Blueprint](#1-architectural-blueprint)
- [2. Technology Stack & Dependencies](#2-technology-stack--dependencies)
- [3. Deep-Dive: Production Patterns](#3-deep-dive-production-patterns)
  - [Pattern A: Polymorphic Document Modeling & Lifecycle Auditing](#pattern-a-polymorphic-document-modeling--lifecycle-auditing)
  - [Pattern B: High-Precision BSON Math & Complex Aggregations](#pattern-b-high-precision-bson-math--complex-aggregations)
  - [Pattern C: Enterprise Index Governance (Compound, Text, TTL)](#pattern-c-enterprise-index-governance-compound-text-ttl)
  - [Pattern D: Multi-Document ACID Transactions & Optimistic Locking](#pattern-d-multi-document-acid-transactions--optimistic-locking)
  - [Pattern E: Thread-Pooled Real-Time Change Streams](#pattern-e-thread-pooled-real-time-change-streams)
  - [Pattern F: Global Resilience & RFC-Compliant Exception Translation](#pattern-f-global-resilience--rfc-compliant-exception-translation)
- [4. Project Structure](#4-project-structure)
- [5. Testing Strategy (The Test Pyramid)](#5-testing-strategy-the-test-pyramid)
- [6. REST API Specification & cURL Collection](#6-rest-api-specification--curl-collection)
- [7. Local Development Quickstart](#7-local-development-quickstart)
- [8. Enterprise Production Tuning Checklist](#8-enterprise-production-tuning-checklist)

---

## 1. Architectural Blueprint

The application follows **Clean Domain-Driven Architecture (Hexagonal)** principles. Core domain models and business invariants remain completely decoupled from transport protocols, external services, and concrete database drivers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                              API / Web Layer                           │
│     (REST Controllers, DTO Validation, Global RFC Exception Handling)   │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                           Application Services                          │
│   (ACID @Transactional Boundaries, @Retryable Concurrency Control)     │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼──────────────────┐   ┌──────────▼───────────────────┐
│          Domain Invariants          │   │      Real-Time Streaming     │
│   (Money Value Object, Auditing,    │   │  (Oplog ChangeStreamListener │
│    Polymorphic Event Hierarchies)   │   │   + Async Internal Bus)      │
└──────────────────┬──────────────────┘   └──────────┬───────────────────┘
                   │                                 │
┌──────────────────▼─────────────────────────────────▼───────────────────┐
│                       Persistence & Aggregation Layer                  │
│    (Spring Data Repositories, Aggregation Pipeline DSL, Custom BSON    │
│    Decimal128 Converters, Programmatic IndexOps Initializer)           │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                   MongoDB 7.0+ Replica Set Cluster                     │
│          (Multi-Doc ACID Transactions, WiredTiger Engine, Oplog)       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack & Dependencies

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java LTS | 17 / 21 | Modern type system, sealed classes, records |
| **Framework** | Spring Boot | 3.3.5 | Core inversion of control and runtime container |
| **Persistence** | Spring Data MongoDB | 4.3.5 | Object-Document mapping, MongoTemplate, Aggregation DSL |
| **Database** | MongoDB | 7.0.x | Document database operating with WiredTiger Replica Set |
| **Resilience** | Spring Retry + AOP | 2.0.10 | Automated backoff and retry for optimistic locking conflicts |
| **Validation** | Jakarta Bean Validation | 3.0.x | Strict request validation at REST boundaries |
| **Testing** | Testcontainers MongoDB | 1.20.2 | True production parity testing using Dockerized replica sets |
| **Mocking** | Mockito 5 + AssertJ | Latest | High-speed unit tests (no Docker required) |
| **Ergonomics** | Project Lombok | 1.18.34 | Boilerplate elimination |

---

## 3. Deep-Dive: Production Patterns

### Pattern A: Polymorphic Document Modeling & Lifecycle Auditing

#### The Problem
In distributed event-driven systems, distinct domain events (e.g., `PaymentEvent`, `UserActivityEvent`) share common tracking metadata (event ID, aggregate ID, timestamp, TTL expiry) while encapsulating completely different payloads. Storing them in separate collections introduces table bloat, prevents atomic cross-event writes, and makes time-ordered event stream queries difficult.

#### The Solution
All events are modeled under an abstract `EventDocument` stored in a single collection (`domain_events`):

```java
@Document(collection = "domain_events")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PaymentEvent.class, name = "PAYMENT"),
    @JsonSubTypes.Type(value = UserActivityEvent.class, name = "USER_ACTIVITY")
})
public abstract class EventDocument extends BaseAuditableDocument {
    @Indexed(unique = true)
    private String eventId;
    private String aggregateId;
    private String aggregateType;
    private Instant eventTimestamp;

    @Indexed(name = "event_ttl_idx", expireAfter = "0s")
    private Instant expireAt; // Automated MongoDB background eviction
}
```

Subclasses declare concise discriminators using `@TypeAlias`:
```java
@TypeAlias("PAYMENT")
public class PaymentEvent extends EventDocument {
    private Money amount;
    private String paymentMethod;
    private String status;
    private String transactionReference;
    private String customerId;
}

@TypeAlias("USER_ACTIVITY")
public class UserActivityEvent extends EventDocument {
    private String userId;
    private String actionType;
    private String ipAddress;
    private Map<String, Object> metadata;
}
```

**Key Enterprise Benefits:**
1. **Clean BSON Document Discriminator**: Instead of writing verbose fully-qualified Java class names (`_class: "com.enterprise.mongodb.domain.model.event.PaymentEvent"`), Spring Data writes `_class: "PAYMENT"`. This decouples the database representation from Java package refactorings and saves storage.
2. **Unified REST Contract**: Jackson `@JsonTypeInfo` and `@JsonSubTypes` align seamlessly with the `@TypeAlias` discriminator, allowing clients to send polymorphic JSON payloads through a single `POST /api/v1/events` endpoint.
3. **Automated Enterprise Auditing**: Subclasses inherit from `BaseAuditableDocument`, which leverages `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, and `@LastModifiedBy`. The `SecurityAuditorAware` bean resolves the current security actor or defaults to the system worker.

---

### Pattern B: High-Precision BSON Math & Complex Aggregations

#### Exact Monetary Representation (`Decimal128`)
Standard IEEE-754 floating point numbers (`double`, `float`) introduce rounding drift:
```
// The floating-point pitfall:
0.1 + 0.2 = 0.30000000000000004
```
In this application, monetary fields use the immutable `Money` Value Object. Spring Data custom converters serialize `Money` directly to **BSON `Decimal128`**:

```java
@WritingConverter
public class MoneyWriteConverter implements Converter<Money, Document> {
    @Override
    public Document convert(@NonNull Money source) {
        Document doc = new Document();
        doc.put("amount", new Decimal128(source.getAmount()));
        doc.put("currency", source.getCurrency().getCurrencyCode());
        return doc;
    }
}
```
Because the amount is stored as BSON `Decimal128`, MongoDB's native aggregation engine executes `$sum`, `$avg`, and `$max` calculations with **128-bit decimal precision**.

#### The 7-Stage Aggregation Pipeline
In `AnalyticsRepositoryImpl.java`, a high-performance analytics pipeline joins transactional ledger entries with customer records across collections:

```mermaid
flowchart LR
    A["1. $match (Debit types & date window)"] --> B["2. $lookup (Cross-collection join: customers)"]
    B --> C["3. $unwind (Flatten joined array)"]
    C --> D["4. $group (Decimal128 sum, avg, max, count)"]
    D --> E["5. $project (Reshape to CustomerSpendingSummary)"]
    E --> F["6. $sort (Order by totalDebitAmount DESC)"]
    F --> G["7. $limit (Top N Spenders)"]
```

```java
Aggregation aggregation = Aggregation.newAggregation(
    // 1. $match
    Aggregation.match(Criteria.where("type").in("DEBIT", "TRANSFER_OUT").and("timestamp").gte(fromDate).lte(toDate)),
    // 2. $lookup
    LookupOperation.newLookup().from("customers").localField("customerId").foreignField("customerNumber").as("customerDetails"),
    // 3. $unwind
    Aggregation.unwind("customerDetails", false),
    // 4. $group
    Aggregation.group("customerId")
        .first("customerDetails.fullName").as("customerName")
        .first("customerDetails.email").as("customerEmail")
        .first("customerDetails.tier").as("customerTier")
        .first("amount.currency").as("currency")
        .sum("amount.amount").as("totalDebitAmount")
        .avg("amount.amount").as("averageDebitAmount")
        .count().as("transactionCount")
        .max("amount.amount").as("maxDebitAmount"),
    // 5. $project
    Aggregation.project()
        .and("_id").as("customerId")
        .and("customerName").as("customerName")
        .and("customerEmail").as("customerEmail")
        .and("customerTier").as("customerTier")
        .and("currency").as("currency")
        .and("totalDebitAmount").as("totalDebitAmount")
        .and("averageDebitAmount").as("averageDebitAmount")
        .and("transactionCount").as("transactionCount")
        .and("maxDebitAmount").as("maxDebitAmount"),
    // 6. $sort
    Aggregation.sort(Sort.Direction.DESC, "totalDebitAmount"),
    // 7. $limit
    Aggregation.limit(limit)
);
```

---

### Pattern C: Enterprise Index Governance (Compound, Text, TTL)

#### Why Disable `auto-index-creation` in Production?
Setting `spring.data.mongodb.auto-index-creation: true` is risky in production:
- Application startup can stall while large collections build indexes.
- Index builds can cause cluster-wide lock contention on high-load nodes.
- Schema changes deployed across multiple microservice replicas may trigger conflicting index builds.

#### The Programmatic Approach
We set `spring.data.mongodb.auto-index-creation: false` in `application.yml` and manage indexes through `MongoIndexInitializer.java` on `ApplicationReadyEvent`:

1. **Compound Index**: Optimizes multi-attribute lookups:
   ```java
   Document keys = new Document("customerId", 1).append("timestamp", -1);
   indexOps.ensureIndex(new CompoundIndexDefinition(keys).named("ledger_cust_time_idx"));
   ```
2. **Weighted Full-Text Search Index**: Enables text search across multiple string attributes with relevance ranking:
   ```java
   TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
       .named("ledger_text_search_idx")
       .onField("description", 3.0f) // Matches in description score 3x higher
       .onField("notes", 1.0f)
       .build();
   indexOps.ensureIndex(textIndex);
   ```
3. **Time-To-Live (TTL) Index**: Enables automatic lifecycle pruning without custom cron scripts:
   ```java
   indexOps.ensureIndex(new Index()
       .on("expireAt", Sort.Direction.ASC)
       .expire(0, TimeUnit.SECONDS)
       .named("event_ttl_idx"));
   ```
   *MongoDB's background TTL thread periodically purges any document where `expireAt <= current_timestamp`.*

---

### Pattern D: Multi-Document ACID Transactions & Optimistic Locking

#### Multi-Document ACID Transactions
Using `MongoTransactionManager`, transfers across accounts and ledger entries execute with strict ACID guarantees:

```java
@Transactional(rollbackFor = Exception.class)
@Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 150, multiplier = 2.0)
)
public TransferResponse transferFunds(TransferRequest request) {
    // 1. Fetch & Validate Source & Destination
    Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber()).orElseThrow(...);
    Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber()).orElseThrow(...);

    // 2. Validate Liquidity Invariant
    if (!fromAccount.getBalance().isGreaterThanOrEqual(transferAmount)) {
        throw new InsufficientFundsException(...);
    }

    // 3. Debit Source Account (Checks & increments @Version)
    fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
    Account savedFromAccount = accountRepository.save(fromAccount);

    // 4. Credit Destination Account (Checks & increments @Version)
    toAccount.setBalance(toAccount.getBalance().add(transferAmount));
    accountRepository.save(toAccount);

    // 5. Dual-Entry Ledger Audit Records
    ledgerEntryRepository.save(debitLedgerEntry);
    ledgerEntryRepository.save(creditLedgerEntry);

    return response;
}
```

#### Atomicity & Rollback Guarantees
If any step fails (e.g., negative balance check, database network partition, or unhandled runtime exception), the entire MongoDB transaction rolls back. Neither account balance is modified, and no ledger entries are persisted.

#### Optimistic Concurrency Control (`@Version`)
High-frequency accounts are susceptible to lost update anomalies. The `Account` document declares:
```java
@Version
private Long version;
```
When two transactions attempt to update the same document concurrently:
1. Thread A reads version 1 and writes version 2.
2. Thread B attempts to write holding version 1.
3. MongoDB rejects Thread B's update because the document version is now 2.
4. Spring Data throws `OptimisticLockingFailureException`.
5. The `@Retryable` annotation intercepts the exception, waits (exponential backoff: 150ms, 300ms), re-reads the fresh version, and re-executes cleanly.

---

### Pattern E: Thread-Pooled Real-Time Change Streams

#### Architecture
Change streams tap into MongoDB's cluster oplog (`local.oplog.rs`), emitting real-time mutation events without database polling overhead:

```
[Account Document Mutation] 
         │ (write)
         ▼
[MongoDB Oplog: local.oplog.rs]
         │ (Change Stream Watcher)
         ▼
[DefaultMessageListenerContainer] (Managed Thread Pool)
         │
         ▼
[MongoChangeStreamListener.onMessage()]
         │
         ▼
[Spring ApplicationEventPublisher] (Decoupled Async Bus)
         ├──> Search Index Synchronizer
         ├──> Redis Cache Invalidation
         └──> Outbound Webhook Dispatcher
```

#### Production Implementation
`MongoChangeStreamListener.java` uses Spring Data's `DefaultMessageListenerContainer`:

```java
@Component
@ConditionalOnProperty(prefix = "app.mongodb.change-stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoChangeStreamListener implements MessageListener<ChangeStreamDocument<Document>, Account> {

    @PostConstruct
    public void startListening() {
        container = new DefaultMessageListenerContainer(mongoTemplate);
        container.start();

        ChangeStreamRequest<Account> request = ChangeStreamRequest.builder(this)
            .collection("accounts")
            .fullDocumentLookup(FullDocument.UPDATE_LOOKUP) // Returns post-image of updated document
            .build();

        subscription = container.register(request, Account.class);
    }

    @Override
    public void onMessage(Message<ChangeStreamDocument<Document>, Account> message) {
        ChangeStreamDocument<Document> rawChange = message.getRaw();
        Account account = message.getBody();

        // Extract mutation metadata & resume token
        OperationType operationType = rawChange.getOperationType();
        String resumeToken = rawChange.getResumeToken() != null ? rawChange.getResumeToken().toJson() : null;

        // Dispatch asynchronously
        eventPublisher.publishEvent(new AccountChangeEvent(...));
    }
}
```

---

### Pattern F: Global Resilience & RFC-Compliant Exception Translation

`GlobalExceptionHandler.java` converts domain and database exceptions into structured RFC 7807/9457 `ApiErrorResponse` payloads:

| Exception | HTTP Status | Error Code | Description |
| :--- | :--- | :--- | :--- |
| `OptimisticLockingFailureException` | `409 CONFLICT` | `CONCURRENT_MODIFICATION_CONFLICT` | Resource was modified concurrently; retry suggested. |
| `DuplicateKeyException` / `MongoWriteException` | `409 CONFLICT` | `DUPLICATE_KEY_VIOLATION` | Unique index constraint violated (e.g., duplicate account number). |
| `InsufficientFundsException` | `422 UNPROCESSABLE_ENTITY`| `INSUFFICIENT_FUNDS` | Invariant violation: debit exceeds account balance. |
| `ResourceNotFoundException` | `404 NOT FOUND` | `RESOURCE_NOT_FOUND` | Account, customer, or event identifier does not exist. |
| `MethodArgumentNotValidException` | `400 BAD REQUEST` | `VALIDATION_ERROR` | Bean validation errors on request body. |

---

## 4. Project Structure

```
spring-boot-mongodb-advanced/
├── pom.xml                                                     # Build manifest with dependency tree
├── mvnw & mvnw.cmd                                             # Self-contained Maven wrapper v3.9.9
├── docker-compose.yml                                          # Single-node replica set + Mongo Express UI
├── requests.http                                               # Executable REST verification suite
├── README.md                                                   # Architecture reference manual
└── src/
    ├── main/
    │   ├── java/com/enterprise/mongodb/
    │   │   ├── MongoAdvancedApplication.java                    # Entrypoint (@EnableAsync, @EnableRetry)
    │   │   ├── changestream/                                   # Real-time Change Streams
    │   │   │   ├── AccountChangeEvent.java                     # Event model for Oplog mutations
    │   │   │   ├── ChangeStreamEventConsumer.java              # Asynchronous downstream event processor
    │   │   │   └── MongoChangeStreamListener.java              # MessageListenerContainer + ChangeStreamRequest
    │   │   ├── config/                                         # Core Configuration & Lifecycle Handlers
    │   │   │   ├── MongoConfig.java                            # TransactionManager, Auditing, Conversions
    │   │   │   ├── MongoIndexInitializer.java                  # Programmatic Compound, Text & TTL Indexes
    │   │   │   ├── SecurityAuditorAware.java                   # AuditorAware for @CreatedBy / @LastModifiedBy
    │   │   │   └── converter/                                  # Custom BSON <-> Value Object Converters
    │   │   │       ├── MoneyReadConverter.java                 # BSON Decimal128 -> Money Value Object
    │   │   │       └── MoneyWriteConverter.java                # Money Value Object -> BSON Decimal128
    │   │   ├── domain/
    │   │   │   ├── common/                                     # Shared Domain Abstractions
    │   │   │   │   ├── BaseAuditableDocument.java              # @Id, @CreatedDate, @LastModifiedDate auditing
    │   │   │   │   └── Money.java                              # Immutable Value Object with currency checks
    │   │   │   └── model/                                      # Domain Entities & Collections
    │   │   │       ├── Account.java                            # @Version Optimistic Locking & Compound Indexes
    │   │   │       ├── AccountStatus.java
    │   │   │       ├── Customer.java
    │   │   │       ├── LedgerEntry.java                        # Dual-entry ledger with Full-Text Search indexing
    │   │   │       ├── LedgerEntryType.java
    │   │   │       └── event/                                  # Polymorphic Document Hierarchy
    │   │   │           ├── EventDocument.java                  # Base Document with @TypeAlias & TTL Index
    │   │   │           ├── PaymentEvent.java                   # @TypeAlias("PAYMENT")
    │   │   │           └── UserActivityEvent.java              # @TypeAlias("USER_ACTIVITY")
    │   │   ├── repository/                                     # Repositories & Custom Aggregations
    │   │   │   ├── AccountRepository.java
    │   │   │   ├── CustomerRepository.java
    │   │   │   ├── EventRepository.java                        # Subtype-filtered polymorphic queries
    │   │   │   ├── LedgerEntryRepository.java                  # Full-Text Search repository integration
    │   │   │   └── aggregation/                                # Complex Aggregation Framework
    │   │   │       ├── AnalyticsRepository.java
    │   │   │       └── impl/AnalyticsRepositoryImpl.java       # Pipeline: $match, $lookup, $unwind, $group, $project, $sort
    │   │   ├── service/                                        # Transactional & Business Logic
    │   │   │   ├── AccountTransferService.java
    │   │   │   ├── AnalyticsService.java
    │   │   │   ├── EventService.java
    │   │   │   └── impl/
    │   │   │       ├── AccountTransferServiceImpl.java         # @Transactional + @Retryable concurrency control
    │   │   │       ├── AnalyticsServiceImpl.java
    │   │   │       └── EventServiceImpl.java
    │   │   └── web/                                            # REST API & Global Resilience
    │   │       ├── controller/
    │   │       │   ├── AccountController.java
    │   │       │   ├── AnalyticsController.java
    │   │       │   └── EventController.java
    │   │       ├── dto/                                        # Validation & Request/Response DTOs
    │   │       │   ├── AccountDto.java
    │   │       │   ├── ApiErrorResponse.java
    │   │       │   ├── CreateAccountRequest.java
    │   │       │   ├── CustomerSpendingSummary.java
    │   │       │   ├── TransferRequest.java
    │   │       │   └── TransferResponse.java
    │   │       └── exception/                                  # Global Exception Handling
    │   │           ├── BusinessException.java
    │   │           ├── GlobalExceptionHandler.java             # RFC 7807 Error translation for Mongo/Concurrency errors
    │   │           ├── InsufficientFundsException.java
    │   │           ├── InvalidTransactionException.java
    │   │           └── ResourceNotFoundException.java
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/enterprise/mongodb/
        │   ├── BaseMongoIntegrationTest.java                   # Testcontainers MongoDB Replica Set Harness
        │   ├── ComplexAggregationIT.java                       # Integration test for 6-stage aggregation pipeline
        │   ├── PolymorphicEventRepositoryIT.java               # Integration test for TypeAlias, Subclasses & Auditing
        │   ├── TransactionAndLockingIT.java                    # Integration test for Multi-document rollback & @Version lock
        │   └── unit/                                           # Fast Unit Tests (Mockito)
        │       ├── AccountTransferServiceTest.java
        │       ├── AnalyticsServiceTest.java
        │       └── MoneyTest.java
        └── resources/
            └── application-test.yml
```

---

## 5. Testing Strategy (The Test Pyramid)

The codebase implements a dual-layer testing approach:

```
          / \
         /   \      Integration Tests (*IT.java)
        / IT  \     Testcontainers MongoDB 7.0 Replica Set
       /───────\    Multi-document Transactions, Aggregations, Oplog
      /         \
     /   Unit    \  Unit Tests (*Test.java)
    /    Tests    \ Mockito 5, AssertJ, Fast execution (<1s)
   /───────────────\ Domain Invariants, Business Rules, Value Objects
```

### 1. Isolated Unit Tests (`*Test.java`)
Fast, memory-based tests running with Mockito. No Docker or external dependencies required:
- `AccountTransferServiceTest`: Validates successful transfers, insufficient funds exceptions, identical account validation, and repository mock interactions.
- `AnalyticsServiceTest`: Validates delegation and parameter routing for analytics reporting.
- `MoneyTest`: Validates currency compatibility checks and boundary conditions.

Execute unit tests:
```powershell
.\mvnw.cmd test -Dtest="*Test"
```

### 2. Testcontainers Integration Tests (`*IT.java`)
Production-parity tests running against a real Dockerized MongoDB 7.0 single-node replica set (`MongoDBContainer("mongo:7.0.5")`):
- `TransactionAndLockingIT`:
  - **Atomicity**: Validates successful debit/credit and dual ledger persistence.
  - **Rollback**: Validates that an error (e.g., overdraft) rolls back all writes across all collections.
  - **Optimistic Locking**: Simulates concurrent modification and asserts `OptimisticLockingFailureException`.
- `PolymorphicEventRepositoryIT`:
  - Validates `PaymentEvent` and `UserActivityEvent` persistence in the same collection (`domain_events`).
  - Verifies deserialization back to concrete subtypes via `@TypeAlias`.
  - Asserts automatic `@CreatedDate` and `@CreatedBy` auditing population.
- `ComplexAggregationIT`:
  - Seeds test data across `customers` and `ledger_entries`.
  - Verifies `$match`, `$lookup`, `$unwind`, `$group`, `$project`, and `$sort` pipeline calculations.

Execute integration tests:
```powershell
.\mvnw.cmd test -Dtest="*IT"
```

---

## 6. REST API Specification & cURL Collection

All endpoints are also available in [`requests.http`](requests.http).

### 1. Create an Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-ALICE-100",
    "customerId": "CUST-ALICE-01",
    "initialDeposit": 1000.00,
    "currency": "USD"
  }'
```

### 2. Execute Atomic Multi-Document Transfer
```bash
curl -X POST http://localhost:8080/api/v1/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "ACC-ALICE-100",
    "toAccountNumber": "ACC-BOB-200",
    "amount": 350.00,
    "currency": "USD",
    "description": "Enterprise consulting milestone #1 payment"
  }'
```

### 3. Test Optimistic Locking Conflict Simulation
```bash
# Simulates a collision by submitting a stale version (e.g. staleVersion=0)
curl -X POST "http://localhost:8080/api/v1/accounts/ACC-ALICE-100/simulate-concurrency?amountDelta=50.00&staleVersion=0"
```
**Expected Response:** `HTTP 409 CONFLICT`
```json
{
  "timestamp": "2026-10-30T18:15:00Z",
  "status": 409,
  "error": "Conflict",
  "errorCode": "CONCURRENT_MODIFICATION_CONFLICT",
  "message": "The requested resource was modified by another concurrent transaction. Please retry with the latest version.",
  "path": "/api/v1/accounts/ACC-ALICE-100/simulate-concurrency"
}
```

### 4. Create Polymorphic PaymentEvent
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "_type": "PAYMENT",
    "eventId": "EVT-PAY-8801",
    "aggregateId": "ACC-ALICE-100",
    "aggregateType": "ACCOUNT",
    "amount": {
      "amount": "350.00",
      "currency": "USD"
    },
    "paymentMethod": "ACH_DIRECT",
    "status": "SETTLED",
    "transactionReference": "TX-REF-100982",
    "customerId": "CUST-ALICE-01"
  }'
```

### 5. Create Polymorphic UserActivityEvent
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "_type": "USER_ACTIVITY",
    "eventId": "EVT-ACT-9902",
    "aggregateId": "ACC-ALICE-100",
    "aggregateType": "ACCOUNT",
    "userId": "USR-ALICE",
    "actionType": "TRANSFER_AUTHORIZED_MFA",
    "ipAddress": "203.0.113.195",
    "userAgent": "Mozilla/5.0 Enterprise Client",
    "metadata": {
      "authMethod": "FIDO2_WEBAUTHN",
      "deviceRiskScore": "LOW"
    }
  }'
```

### 6. Query Complex Aggregation (Top Spenders)
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/top-spenders?limit=5"
```
**Sample Output:**
```json
[
  {
    "customerId": "CUST-ALICE-01",
    "customerName": "Alice Wonderland",
    "customerEmail": "alice@enterprise.com",
    "customerTier": "PLATINUM",
    "totalDebitAmount": 350.00,
    "averageDebitAmount": 350.00,
    "transactionCount": 1,
    "maxDebitAmount": 350.00,
    "currency": "USD"
  }
]
```

---

## 7. Local Development Quickstart

### Prerequisites
- **JDK 17 or JDK 21+**
- **Docker Desktop** (for local replica set and testcontainers)

### Step 1: Start MongoDB Replica Set
```bash
# In the project directory:
docker compose up -d
```
This spins up:
- **MongoDB 7.0** on `localhost:27017` with replica set `rs0` automatically initiated.
- **Mongo Express Web UI** on [http://localhost:8081](http://localhost:8081).

### Step 2: Build & Verify the Project
```powershell
.\mvnw.cmd clean compile
```

### Step 3: Run the Spring Boot Application
```powershell
.\mvnw.cmd spring-boot:run
```

---

## 8. Enterprise Production Tuning Checklist

Before deploying this architecture to high-throughput production clusters (e.g., MongoDB Atlas, Kubernetes Operator), review these operational recommendations:

### 1. Connection Pool Sizing
In `application.yml`, configure MongoDB client connection pool parameters based on available vCPUs and expected concurrency:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://user:pass@mongo-primary:27017,mongo-sec1:27017/enterprise_mongodb?replicaSet=rs0&maxPoolSize=150&minPoolSize=20&maxIdleTimeMS=30000&waitQueueTimeoutMS=5000
```

### 2. Write & Read Concerns
- **Transactions**: Multi-document transactions automatically inherit `w: "majority"`, ensuring committed data is replicated to a quorum before returning success.
- **Read Preference**: For heavy reporting aggregation queries that can tolerate slight replication lag, route reads to secondaries using `readPreference=secondaryPreferred` on read-only repositories.

### 3. Change Stream Resume Tokens
In distributed deployments across restarts, persist the resume token (`rawResumeToken` from `AccountChangeEvent`) to an external cache (e.g., Redis or an internal checkpoint collection). Pass the stored token to `.startAfter(resumeToken)` when recreating the `ChangeStreamRequest` to avoid missing events during rolling restarts.

### 4. Index Lifecycle Governance
Never run index builds directly on the primary in high-throughput clusters. Use **rolling index builds** across secondary nodes, or schedule programmatic initializers during deployment maintenance windows.

### 5. Production Metrics & Observability
Spring Boot Actuator is pre-configured. Scrape Prometheus metrics at:
```
GET http://localhost:8080/actuator/prometheus
```
Key MongoDB metrics to monitor:
- `mongodb.driver.pool.size`
- `mongodb.driver.pool.waitqueuesize`
- `mongodb.driver.commands.duration`
