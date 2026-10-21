# MS1 Microservices Workspace

This repository contains two independent Spring Boot microservices:

1. **`product-service`** (Runs on port `8081`, manages product catalog)
2. **`order-service`** (Runs on port `8082`, places orders and communicates with `product-service` via `RestClient`)

---

## Project Structure

```text
MS1/
├── pom.xml                               # Root parent / aggregator POM
├── product-service/                      # Microservice 1 (Port 8081)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/productservice/
│       │   ├── Product.java
│       │   ├── ProductRepository.java
│       │   ├── ProductController.java
│       │   ├── ProductNotFoundException.java
│       │   ├── InsufficientStockException.java
│       │   └── ProductServiceApplication.java
│       └── resources/
│           └── application.properties    # port 8081, jdbc:h2:mem:productdb
└── order-service/                        # Microservice 2 (Port 8082)
    ├── pom.xml
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
            └── application.properties    # port 8082, jdbc:h2:mem:orderdb
```

---

## How to Run

### Option A: Using the Launcher Script (Recommended)

You can launch both microservices simultaneously with a single command from the project root:

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

### Option B: Manual Execution

#### Step 1: Build the entire project (Optional first step)

```bash
mvn clean compile
```

#### Step 2: Start each microservice in separate terminals

##### Terminal 1: Run Product Service (Port 8081)

```bash
cd c:\Users\yahya\Desktop\MS1\product-service
mvn spring-boot:run
```

- Available at: `http://localhost:8081/products`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- H2 Console: `http://localhost:8081/h2-console` (JDBC: `jdbc:h2:mem:productdb`)

##### Terminal 2: Run Order Service

```bash
cd c:\Users\yahya\Desktop\MS1\order-service
mvn spring-boot:run
```

- Available at: `http://localhost:8082/orders`
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`
- H2 Console: `http://localhost:8082/h2-console` (JDBC: `jdbc:h2:mem:orderdb`)

---

## Swagger UI Interactive Documentation

Both microservices have Swagger UI enabled for testing APIs directly in your browser without needing curl or Postman:

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
- `product-service` owns its own database (`jdbc:h2:mem:productdb`).
- `order-service` owns its own database (`jdbc:h2:mem:orderdb`).
- Neither service can access the other's database directly.

Spring's `@Transactional` annotation only manages the local connection to `orderdb`. It cannot span network calls to rollback changes made in `productdb`.

### The Dual-Write Failure Scenario
1. `order-service` calls `POST http://localhost:8081/products/1/deduct-stock?quantity=2`.
2. `product-service` decrements stock from 10 to 8 and commits the change to `productdb`.
3. If `order-service` encounters a local error (database constraint failure, unhandled runtime exception, or process crash) right before or during `orderRepository.save(order)`:
   - The local order transaction fails.
   - Without compensation, `productdb` retains the deducted stock (stock = 8), while `orderdb` has 0 record of the order.
   - **Result:** Two laptops disappear from inventory without any order existing.

### How This Project Solves It: Compensating Transaction (Saga Pattern)
To ensure **eventual consistency** across both services:
1. `product-service` provides a compensating endpoint:  
   `POST http://localhost:8081/products/{id}/restore-stock?quantity={qty}`
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


