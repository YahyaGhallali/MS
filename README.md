# MS1 Microservices Workspace: Event-Driven Architecture with Apache Kafka & Saga Choreography

This repository contains an end-to-end microservices ecosystem built with **Spring Boot 3.4.3**, **Spring Cloud 2024.0.0 (Moorgate)**, **Apache Kafka (KRaft mode)**, **Netflix Eureka**, and **Spring Cloud Gateway**, backed by PostgreSQL.

---

## Architecture Overview

```text
                               ┌────────────────────────────────┐
                               │  Eureka Discovery Server       │
                               │  http://localhost:8761         │
                               └───────────▲────────────────────┘
                                           │ Heartbeats & Registration
                                           │
  Client Requests ──────► ┌────────────────┴──────────────────┐
  (http://localhost:8080) │  Spring Cloud API Gateway         │
                          │  - Port 8080 (Single Public Entry)│
                          │  - Global Logging & Correlation ID│
                          └───────────┬──────────────┬────────┘
                   /products/**       │              │  /orders/**
                (lb://product-service)│              │  (lb://order-service)
                                      ▼              ▼
                    ┌─────────────────────────┐   ┌─────────────────────────┐
                    │ product-service         │   │ order-service           │
                    │ - Replica 1 & 2         │   │ - Port 8082 (Internal)  │
                    │ - Port 8081 (Internal)  │   │ - Transactional Outbox  │
                    │ - Idempotent Consumer   │   │ - OutboxPublisher Task  │
                    └───────────┬─────────────┘   └───────────┬─────────────┘
                                │                             │
                                ▼                             ▼
                    ┌─────────────────────────┐   ┌─────────────────────────┐
                    │ product-db (PostgreSQL) │   │ order-db (PostgreSQL)   │
                    │ - products table        │   │ - orders table          │
                    │ - processed_events      │   │ - outbox_events         │
                    └───────────┬─────────────┘   └───────────┬─────────────┘
                                │                             │
                                └──────────────┬──────────────┘
                                               │
                                               ▼
                              ┌─────────────────────────────────┐
                              │      Apache Kafka Broker        │
                              │      - Port 9092 (KRaft Mode)   │
                              │  - order-created-topic          │
                              │  - order-created-topic.DLT      │
                              │  - stock-reserved-topic         │
                              │  - stock-rejected-topic         │
                              └─────────────────────────────────┘
```

---

## Asynchronous Saga Choreography Flow

```text
 1. POST /orders (Client)
    │
    ▼
 [order-service]
    │  (Atomic Local DB Transaction)
    ├──► 1. Save Order (status: PENDING)
    └──► 2. Save OutboxEvent (topic: order-created-topic)
    │
    ◄── Return 202 Accepted { id: 10, status: "PENDING" }
    │
    ▼
 [OutboxPublisher] (@Scheduled background task)
    │  Polls unprocessed events, publishes to Kafka
    ▼
  Kafka Topic: order-created-topic
    │
    ▼
 [product-service: OrderCreatedConsumer]
    ├── Idempotency Check: Query processed_events(eventId)
    │     └── If already processed, skip duplicate message
    ├── Record eventId in processed_events table
    └── Check product stock in productdb
          │
          ├── (Stock Sufficient)
          │     ├── Deduct stock
          │     └── Publish StockReservedEvent ──► Kafka: stock-reserved-topic
          │
          └── (Product Not Found OR Insufficient Stock)
                └── Publish StockRejectedEvent ──► Kafka: stock-rejected-topic
    │
    ▼
 [order-service: StockEventsConsumer]
    ├── If StockReservedEvent ──► Update order status to CONFIRMED
    └── If StockRejectedEvent ──► Update order status to REJECTED
    │
    ▼
 Client Polls GET /orders/10 ──► { "id": 10, "status": "CONFIRMED" }
```

---

## Key Architectural Concepts & Trade-offs

### 1. Event Contract Trade-off: Deliberate Duplication vs. Shared Library
- **Decision**: Event contracts (`OrderCreatedEvent`, `StockReservedEvent`, `StockRejectedEvent`) are **deliberately duplicated** across `order-service` and `product-service` instead of placing them in a shared Maven library (`common-events.jar`).
- **Rationale**:
  - *Shared Module coupling*: A shared Java module creates tight compile-time and deployment coupling. Updating a schema in the shared library forces coordinated recompilation, version upgrades, and redeployment of both microservices, defeating independent deployability.
  - *Deliberate Duplication*: Services remain completely independent at build time and runtime. Using Jackson JSON serialization with standard properties ensures the **Tolerant Reader pattern**, allowing each service to evolve internally without breaking others.

### 2. Transactional Outbox Pattern (No Dual-Write Problem)
- Direct publishing to Kafka within a REST request controller causes the classic **dual-write problem**: if the database commit succeeds but the Kafka broker is temporarily unreachable (or vice versa), the system is left in an inconsistent state.
- **Solution**:
  - `OrderController` saves the `Order` entity (status `PENDING`) and an `OutboxEvent` record inside a single Spring `@Transactional` database commit.
  - A background `@Scheduled` worker (`OutboxPublisher`) reads unprocessed outbox events, delivers them to Kafka via `KafkaTemplate`, and marks them as processed upon broker acknowledgment.
  - This provides **guaranteed at-least-once message delivery**.

### 3. Idempotent Consumer Pattern
- Because Kafka guarantees at-least-once delivery, consumers must handle duplicate messages gracefully (e.g., due to network blips or consumer rebalances).
- In `product-service`, `OrderCreatedConsumer` checks the `processed_events` table using the unique `eventId`:
  - If `processedEventRepository.existsById(eventId)` is true, the message is safely skipped.
  - Otherwise, the `eventId` is recorded and stock reservation proceeds.

### 4. Error Handling, Exponential Backoff & Dead-Letter Topic (DLT)
- **Domain Rejections vs. Infrastructure Errors**:
  - *Domain Rejections* (e.g. `ProductNotFoundException` or `InsufficientStockException`) are expected business outcomes. They are **not** retried; `product-service` publishes a `StockRejectedEvent` and commits the offset.
  - *Poison Messages / Deserialization Failures*: Handled by Spring Kafka's `DefaultErrorHandler` configured with `DeadLetterPublishingRecoverer` and `ExponentialBackOff` (initial 1s, multiplier 2.0, max 3 attempts). If attempts are exhausted, the poison message is forwarded to `order-created-topic.DLT` to prevent head-of-line blocking.

### 5. Asynchronous Client Interaction (Polling)
- Instead of blocking HTTP connections waiting for cross-service calls, `POST /orders` immediately responds with **`202 Accepted`** and `{ "status": "PENDING" }`.
- Clients poll `GET /orders/{id}` to observe status transitions (`PENDING` -> `CONFIRMED` or `REJECTED`).

---

## Project Structure

```text
MS1/
├── pom.xml                               # Root parent POM with Spring Cloud 2024.0.0 BOM
├── docker-compose.yml                    # Multi-container orchestration (Eureka, Gateway, Kafka, DBs, Services)
├── run-services.bat                      # Windows CMD launcher (Docker Compose)
├── run-services.ps1                      # PowerShell launcher (Docker Compose)
├── run-services.sh                       # Bash launcher (Docker Compose)
├── discovery-server/                     # Eureka Discovery Server (Port 8761)
│   ├── Dockerfile
│   └── src/main/
├── api-gateway/                          # Reactive Spring Cloud Gateway (Port 8080)
│   ├── Dockerfile
│   └── src/main/java/com/example/apigateway/
│       ├── ApiGatewayApplication.java
│       └── LoggingAndCorrelationFilter.java
├── product-service/                      # Catalog & Inventory Service (2 Replicas, Port 8081 Internal)
│   ├── Dockerfile
│   └── src/main/java/com/example/productservice/
│       ├── config/KafkaConfig.java       # Topics, DLT, DefaultErrorHandler, JSON Converter
│       ├── consumer/OrderCreatedConsumer.java # Idempotent Kafka consumer
│       ├── event/                        # Deliberately duplicated event contracts
│       ├── idempotency/                  # ProcessedEvent entity & repository
│       ├── ProductController.java
│       └── InstanceIdFilter.java
└── order-service/                        # Order Service (Port 8082 Internal)
    ├── Dockerfile
    └── src/main/java/com/example/orderservice/
        ├── config/KafkaConfig.java       # Kafka topic declarations & converters
        ├── consumer/StockEventsConsumer.java # Listens for StockReserved/StockRejected
        ├── event/                        # Deliberately duplicated event contracts
        ├── outbox/                       # OutboxEvent, OutboxEventRepository, OutboxPublisher
        ├── OrderController.java          # 202 Accepted + Outbox write + GET by ID
        └── OrderServiceApplication.java  # @EnableScheduling
```

---

## Port Summary

| Component | Port | Host Access | Description |
|---|---|---|---|
| **API Gateway** | `8080` | `http://localhost:8080` | **Sole public API entry point** |
| **Eureka Server** | `8761` | `http://localhost:8761` | **Eureka Discovery Dashboard & Registry** |
| **Apache Kafka** | `9092` | *Internal / Compose* | Message broker in KRaft mode |
| **product-service** | `8081` | *Internal only* | Replicated (2 instances), load balanced, Kafka consumer |
| **order-service** | `8082` | *Internal only* | Transactional outbox publisher & Kafka consumer |
| **product-db** | `5432` | *Internal only* | PostgreSQL database for `product-service` |
| **order-db** | `5432` | *Internal only* | PostgreSQL database for `order-service` |

---

## How to Run

### Option A: Using Docker Compose (Recommended)

Start the entire ecosystem with a single command from the project root:

```bash
docker compose up --build
```

To run in detached (background) mode:

```bash
docker compose up -d --build
```

View live logs (including outbox publication and Kafka consumer events):

```bash
docker compose logs -f
```

Stop and tear down the stack:

```bash
docker compose down
```

Reset database volumes:

```bash
docker compose down -v
```

---

### Option B: Using Launcher Scripts

- **Windows Batch (CMD / Double-click):**
  ```cmd
  run-services.bat
  ```
- **PowerShell:**
  ```powershell
  .\run-services.ps1
  ```
- **Git Bash / Linux / macOS:**
  ```bash
  chmod +x run-services.sh
  ./run-services.sh
  ```

---

## Verifying Features & Testing Endpoints

### 1. View Eureka Service Registry
Open your browser and navigate to:
**`http://localhost:8761`**

You will see:
- `API-GATEWAY` (1 instance)
- `ORDER-SERVICE` (1 instance)
- `PRODUCT-SERVICE` (2 instances with individual container IDs)

---

### 2. View Product Catalog
`GET http://localhost:8080/products`

Response:
```json
[
  { "id": 1, "name": "Laptop", "price": 999.99, "stock": 10 },
  { "id": 2, "name": "Mouse", "price": 29.99, "stock": 50 },
  { "id": 3, "name": "Keyboard", "price": 49.99, "stock": 25 }
]
```

---

### 3. Place Order & Observe Asynchronous Saga Completion
1. Submit an order:
   ```bash
   curl -i -X POST http://localhost:8080/orders \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 2}'
   ```
   **Immediate HTTP Response**: `202 Accepted`
   ```json
   {
     "id": 1,
     "productId": 1,
     "quantity": 2,
     "status": "PENDING"
   }
   ```

2. Inspect container logs:
   ```bash
   docker compose logs -f order-service product-service
   ```
   - `order-service` logs: `[Outbox] Published OrderCreatedEvent [eventId=..., orderId=1] to Kafka topic order-created-topic`
   - `product-service` logs: `[product-service] Reserved 2 units of product 'Laptop'. Stock updated from 10 to 8.`
   - `order-service` logs: `[Order Service] Received StockReservedEvent for order ID: 1. Status updated to CONFIRMED.`

3. Poll order status:
   ```bash
   curl -i http://localhost:8080/orders/1
   ```
   **HTTP 200 OK**:
   ```json
   {
     "id": 1,
     "productId": 1,
     "quantity": 2,
     "status": "CONFIRMED"
   }
   ```

---

### 4. Test Saga Rejection (Insufficient Stock)
1. Submit an order exceeding available inventory:
   ```bash
   curl -i -X POST http://localhost:8080/orders \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 9999}'
   ```
   Returns `202 Accepted` with `"status": "PENDING"`.
2. `product-service` emits `StockRejectedEvent`:
   `[product-service] Insufficient stock for product Laptop [available=8, requested=9999]. Rejecting stock for orderId=2.`
3. Poll order status:
   ```bash
   curl -i http://localhost:8080/orders/2
   ```
   Response shows order resolved to `"status": "REJECTED"`:
   ```json
   {
     "id": 2,
     "productId": 1,
     "quantity": 9999,
     "status": "REJECTED"
   }
   ```

---

### 5. Correlation ID Tracing
Send a custom Correlation ID:
```bash
curl -i -H "X-Correlation-Id: trace-abc-123" http://localhost:8080/products
```
The Gateway logs the incoming request with that ID, passes it to the downstream service, and returns `X-Correlation-Id: trace-abc-123` in the HTTP response.

---

## Running Automated Tests

All automated unit and integration tests across all modules (Eureka, API Gateway, Product Service, Order Service) can be executed with:

```bash
mvn test
```
