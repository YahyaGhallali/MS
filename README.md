# MS1 Microservices Workspace

This repository contains two independent Spring Boot microservices containerized with Docker and backed by PostgreSQL:

1. **`product-service`** (Runs on port `8081`, backed by `productdb`, manages product catalog)
2. **`order-service`** (Runs on port `8082`, backed by `orderdb`, places orders and communicates with `product-service` via `RestClient`)

---

## Project Structure

```text
MS1/
├── docker-compose.yml                    # Multi-container orchestration (DBs + Services)
├── pom.xml                               # Root parent / aggregator POM
├── run-services.bat                      # Windows CMD launcher (Docker Compose)
├── run-services.ps1                      # PowerShell launcher (Docker Compose)
├── run-services.sh                       # Bash launcher (Docker Compose)
├── product-service/                      # Microservice 1 (Port 8081)
│   ├── Dockerfile                        # Multi-stage Docker build (Maven -> Slim JRE)
│   ├── .dockerignore
│   ├── pom.xml                           # Spring Boot 3.4.3 & PostgreSQL driver
│   └── src/main/
│       ├── java/com/example/productservice/
│       │   ├── Product.java
│       │   ├── ProductRepository.java
│       │   ├── ProductController.java
│       │   ├── ProductNotFoundException.java
│       │   ├── InsufficientStockException.java
│       │   └── ProductServiceApplication.java
│       └── resources/
│           └── application.properties    # port 8081, externalized PostgreSQL datasource
└── order-service/                        # Microservice 2 (Port 8082)
    ├── Dockerfile                        # Multi-stage Docker build (Maven -> Slim JRE)
    ├── .dockerignore
    ├── pom.xml                           # Spring Boot 3.4.3 & PostgreSQL driver
    └── src/main/
        ├── java/com/example/orderservice/
        │   ├── Order.java
        │   ├── OrderRepository.java
        │   ├── OrderController.java
        │   ├── ProductDTO.java
        │   ├── ProductNotFoundException.java
        │   ├── InsufficientStockException.java
        │   ├── OrderProcessingException.java
        │   └── OrderServiceApplication.java
        └── resources/
            └── application.properties    # port 8082, externalized PostgreSQL & product URL
```

---

## How to Run

### Option A: Using Docker Compose (Recommended)

Run the entire microservices stack (PostgreSQL databases + Spring Boot services) with a single command from the project root:

```bash
docker compose up --build
```

To run in detached (background) mode:

```bash
docker compose up -d --build
```

To view logs:

```bash
docker compose logs -f
```

To stop and remove containers and networks:

```bash
docker compose down
```

To reset all data (including database volumes):

```bash
docker compose down -v
```

---

### Option B: Using Launcher Scripts

Quick-start scripts at the repository root invoke Docker Compose:

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

### Option C: Manual Local Execution (Maven)

If running directly on the host without containerizing the services, ensure PostgreSQL is running locally on port 5432 with databases `productdb` and `orderdb`.

#### Step 1: Compile the project

```bash
mvn clean compile
```

#### Step 2: Start each microservice in separate terminals

##### Terminal 1: Run Product Service (Port 8081)

```bash
cd product-service
mvn spring-boot:run
```

- Available at: `http://localhost:8081/products`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`

##### Terminal 2: Run Order Service (Port 8082)

```bash
cd order-service
mvn spring-boot:run
```

- Available at: `http://localhost:8082/orders`
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`

---

## Externalized Configuration

All database credentials and service URLs are externalized with default fallbacks for local execution:

| Service | Environment Variable | Description | Default Value |
|---|---|---|---|
| `product-service` | `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/productdb` |
| `product-service` | `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `product-service` | `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `order-service` | `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/orderdb` |
| `order-service` | `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `order-service` | `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `order-service` | `PRODUCT_SERVICE_URL` | Base URL of product-service | `http://localhost:8081` |

In `docker-compose.yml`, Docker service names are used automatically:
- `product-service` connects to `jdbc:postgresql://product-db:5432/productdb`
- `order-service` connects to `jdbc:postgresql://order-db:5432/orderdb`
- `order-service` communicates with `http://product-service:8081`

---

## Startup Order & Healthchecks

PostgreSQL databases take several seconds to initialize on startup. To prevent Spring Boot services from failing when starting before the database is ready, `docker-compose.yml` configures healthchecks:

- **`product-db` & `order-db`:**
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d <dbname>"]
    interval: 5s
    timeout: 5s
    retries: 5
  ```
- **Service dependencies:**
  - `product-service` waits for `product-db` with `condition: service_healthy`.
  - `order-service` waits for `order-db` with `condition: service_healthy` and `product-service` with `condition: service_started`.

---

## Swagger UI Interactive Documentation

Both microservices have Swagger UI enabled for testing APIs directly in your browser:

| Service | Swagger UI URL | OpenAPI Spec |
|---|---|---|
| **`product-service`** (Port 8081) | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| **`order-service`** (Port 8082) | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |

> **Tip:** In the Swagger UI dashboard, click any endpoint and hit **"Try it out"** -> **"Execute"** to send live requests.

---

## Endpoints & Testing

### 1. Check Initial Products & Stock (product-service)

`GET http://localhost:8081/products`

Returns:

```json
[
  { "id": 1, "name": "Laptop", "price": 999.99, "stock": 10 },
  { "id": 2, "name": "Mouse", "price": 29.99, "stock": 50 },
  { "id": 3, "name": "Keyboard", "price": 49.99, "stock": 25 }
]
```

### 2. Place an Order (order-service)

Order 2 units of Laptop (`productId: 1`):

`POST http://localhost:8082/orders`  
Request body:

```json
{
  "productId": 1,
  "quantity": 2
}
```

Response:

```json
{
  "id": 1,
  "productId": 1,
  "quantity": 2,
  "unitPrice": 999.99,
  "totalPrice": 1999.98,
  "status": "CREATED"
}
```

### 3. Verify Stock Decrement (product-service)

Inspect Laptop stock after placing the order:

`GET http://localhost:8081/products/1`

Response (`stock` has decreased from `10` to `8`):

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "stock": 8
}
```

### 4. Insufficient Stock (order-service)

Try ordering more items than available in stock:

`POST http://localhost:8082/orders`  
Request body:

```json
{
  "productId": 1,
  "quantity": 999
}
```

Response: `HTTP 400 Bad Request`

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot place order: Insufficient stock for product ID 1",
  "path": "/orders"
}
```

### 5. Product Not Found (order-service)

`POST http://localhost:8082/orders`  
Request body:

```json
{
  "productId": 999,
  "quantity": 1
}
```

Response: `HTTP 404 Not Found`

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 999 not found in product-service!",
  "path": "/orders"
}
```

---

## Microservices Architecture: Database-Per-Service & Distributed Consistency

### The Core Problem: Why Two Databases Cannot Share One Transaction
In a monolithic application with a single relational database, placing an order and decrementing stock can be wrapped in a single `@Transactional` method. If any step fails, the database automatically rolls back both tables atomically (ACID guarantees).

In a **microservices architecture** using the **Database-per-Service pattern**:
- `product-service` owns its own database (`productdb`).
- `order-service` owns its own database (`orderdb`).
- Neither service can access the other's database directly.

Spring's `@Transactional` annotation only manages the local connection to `orderdb`. It cannot span network calls to rollback changes made in `productdb`.

### The Dual-Write Failure Scenario
1. `order-service` calls `POST http://product-service:8081/products/1/deduct-stock?quantity=2`.
2. `product-service` decrements stock from 10 to 8 and commits the change to `productdb`.
3. If `order-service` encounters a local error (database constraint failure, unhandled runtime exception, or process crash) right before or during `orderRepository.save(order)`:
   - The local order transaction fails.
   - Without compensation, `productdb` retains the deducted stock (stock = 8), while `orderdb` has 0 record of the order.
   - **Result:** Two laptops disappear from inventory without any order existing.

### How This Project Solves It: Compensating Transaction (Saga Pattern)
To ensure **eventual consistency** across both services:
1. `product-service` provides a compensating endpoint:  
   `POST /products/{id}/restore-stock?quantity={qty}`
2. In `OrderController.java`, local order saving is wrapped in a failure handler:
   ```java
   try {
       return orderRepository.save(order);
   } catch (Exception ex) {
       // Compensating Transaction (Saga): Restore deducted stock in product-service
       compensateStock(order.getProductId(), order.getQuantity());
       throw new OrderProcessingException("Order save failed. Compensating transaction executed: Stock restored.", ex);
   }
   ```
3. If saving the order fails locally, `order-service` automatically invokes the compensating action to restore inventory, preserving stock consistency across independent services.
