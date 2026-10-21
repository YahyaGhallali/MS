# Order Service (Spring Boot & Apache Kafka)

Microservice for managing orders, backed by PostgreSQL (`orderdb`), registered with **Eureka Discovery Server**, and integrated into an asynchronous **Event-Driven Saga** using **Apache Kafka** and the **Transactional Outbox Pattern**.

---

## Architecture & Event-Driven Saga

### 1. Asynchronous Saga Flow
```text
Client ──POST /orders──► [OrderController]
                                │ (Atomic Transaction)
                                ├──► Save Order (Status: PENDING)
                                └──► Save OutboxEvent (order-created-topic)
                                │
                      [HTTP 202 Accepted]
                                │
                                ▼
                     [OutboxPublisher (Scheduled)]
                                │
                                ▼ Kafka: order-created-topic
                     [Product Service Consumer]
                     - Idempotency check (processed_events table)
                     - Check stock & reserve
                                │
             ┌──────────────────┴──────────────────┐
     (Success)                               (Failure)
             ▼ Kafka: stock-reserved-topic         ▼ Kafka: stock-rejected-topic
    [StockEventsConsumer]                 [StockEventsConsumer]
             │                                     │
   Status -> CONFIRMED                   Status -> REJECTED
```

### 2. Transactional Outbox Pattern
To prevent dual-write inconsistencies (where a database write succeeds but message broker publish fails, or vice versa):
- **Local Transaction**: The order (with status `PENDING`) and the event payload are saved within the same PostgreSQL ACID transaction in `order_service` and `outbox_events` tables.
- **Relay / Publisher**: The `@Scheduled` `OutboxPublisher` polls unprocessed outbox records, publishes them to Kafka's `order-created-topic`, and marks them as processed upon acknowledgment.
- **Guaranteed Delivery**: Guarantees at-least-once message delivery to Kafka without distributed 2PC transactions.

### 3. Event Contract Trade-off: Deliberate Duplication vs. Shared Library
- **Decision**: Event contracts (`OrderCreatedEvent`, `StockReservedEvent`, `StockRejectedEvent`) are **deliberately duplicated** in `order-service` and `product-service`.
- **Trade-off Analysis**:
  - *Shared Library (`common-events.jar`)*: Enforces strict compile-time type safety across services, but introduces tight build-time coupling. Any change or version bump requires releasing and rebuilding downstream services, violating microservice independence.
  - *Deliberate Duplication*: Services remain completely autonomous with independent deployment lifecycles and build pipelines. By adhering to the **Tolerant Reader pattern** and JSON serialization, services only consume fields they require without tight coupling.

---

## Project Structure

```text
order-service/
├── Dockerfile                          # Multi-stage Docker build
├── .dockerignore                       # Docker ignore patterns
├── pom.xml                             # Spring Boot 3.4.3, Spring Kafka, Eureka Client, PostgreSQL
└── src/
    ├── main/
    │   ├── java/com/example/orderservice/
    │   │   ├── Order.java                      # JPA Entity with status (PENDING, CONFIRMED, REJECTED)
    │   │   ├── OrderRepository.java            # JPA Repository
    │   │   ├── OrderController.java            # REST Controller (202 Accepted + Outbox)
    │   │   ├── ProductDTO.java                 # DTO
    │   │   ├── config/
    │   │   │   └── KafkaConfig.java            # Kafka Topics & JsonConverter
    │   │   ├── event/                          # Event Contracts (Duplicated)
    │   │   │   ├── OrderCreatedEvent.java
    │   │   │   ├── StockReservedEvent.java
    │   │   │   └── StockRejectedEvent.java
    │   │   ├── outbox/                         # Transactional Outbox Pattern
    │   │   │   ├── OutboxEvent.java
    │   │   │   ├── OutboxEventRepository.java
    │   │   │   └── OutboxPublisher.java
    │   │   ├── consumer/                       # Saga Response Consumer
    │   │   │   └── StockEventsConsumer.java    # Listens for stock-reserved & stock-rejected
    │   │   └── OrderServiceApplication.java    # @EnableScheduling & Bootstrap
    │   └── resources/
    │       └── application.properties          # Port 8082, Kafka bootstrap, PostgreSQL
    └── test/                                   # Unit and Mockito test suite
```

---

## How to Run

### Option A: Using Docker Compose (Recommended)
From the project root directory:
```bash
docker compose up --build
```
Access via API Gateway at `http://localhost:8080/orders`.

### Option B: Local Maven Execution
With Kafka running on port 9092, PostgreSQL on 5432, and Eureka on 8761:
```bash
cd order-service
mvn spring-boot:run
```

---

## API Endpoints (via API Gateway)

| HTTP Method | Gateway URL | Response Status | Description |
| --- | --- | --- | --- |
| **POST** | `http://localhost:8080/orders` | `202 Accepted` | Creates order with `PENDING` status and stages outbox event |
| **GET** | `http://localhost:8080/orders` | `200 OK` | Get all orders |
| **GET** | `http://localhost:8080/orders/{id}` | `200 OK` | Poll order status (`PENDING` -> `CONFIRMED` or `REJECTED`) |

### Polling Workflow Example:
1. Client sends:
   ```bash
   curl -i -X POST http://localhost:8080/orders \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 2}'
   ```
   Receives `HTTP/1.1 202 Accepted` with payload `{ "id": 10, "status": "PENDING", ... }`.
2. Client polls:
   ```bash
   curl -i http://localhost:8080/orders/10
   ```
   After `product-service` reserves stock, response updates to:
   ```json
   {
     "id": 10,
     "productId": 1,
     "quantity": 2,
     "status": "CONFIRMED"
   }
   ```

