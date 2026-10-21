# Order Service (Spring Boot)

Microservice for managing orders, running on **port 8082** with its own PostgreSQL database (`orderdb`).

---

## Project Structure

```text
order-service/
├── Dockerfile                          # Multi-stage Docker build
├── .dockerignore                       # Docker ignore patterns
├── pom.xml                             # Maven build & dependencies (Spring Boot 3.4.3, PostgreSQL)
└── src/
    └── main/
        ├── java/com/example/orderservice/
        │   ├── Order.java                      # Entity (id, productId, quantity, unitPrice, totalPrice, status)
        │   ├── OrderRepository.java            # JPA Repository for orders
        │   ├── OrderController.java            # REST Controller with POST /orders using RestClient
        │   ├── ProductDTO.java                 # DTO to map response from product-service
        │   ├── ProductNotFoundException.java   # Custom 404 Exception
        │   ├── InsufficientStockException.java # Custom 400 Exception
        │   ├── OrderProcessingException.java   # Custom 500 Exception for Saga compensation
        │   └── OrderServiceApplication.java    # Main class & RestClient bean
        └── resources/
            └── application.properties          # Port (8082), PostgreSQL config, product.service.url
```

---

## How to Run

### Option A: Using Docker Compose (Recommended)

From the project root directory:

```bash
docker compose up --build
```

This starts `product-db`, `order-db`, `product-service` (port 8081), and `order-service` (port 8082) together with healthchecks.

### Option B: Local Maven Execution

If you have PostgreSQL running locally on port 5432 with databases `productdb` and `orderdb`:

#### Terminal 1: Start `product-service` (Port 8081)

```bash
cd product-service
mvn spring-boot:run
```

#### Terminal 2: Start `order-service` (Port 8082)

```bash
cd order-service
mvn spring-boot:run
```

---

## API Endpoints

| HTTP Method | URL | Description |
| --- | --- | --- |
| **POST** | `http://localhost:8082/orders` | Create an order (atomically deducts stock from `product-service`, with compensating rollback if local save fails) |
| **GET** | `http://localhost:8082/orders` | Get all orders |
| **GET** | `http://localhost:8082/orders/{id}` | Get order by ID |
| **GET** | `http://localhost:8082/swagger-ui.html` | Swagger UI documentation |

---

## Testing Scenarios

### 1. Place an Order for an Existing Product (Success - 200 OK)

In `product-service`, product ID `1` is the **Laptop** ($999.99, initial stock: 10).

**Request:**
`POST http://localhost:8082/orders`

```json
{
  "productId": 1,
  "quantity": 2
}
```

**Response:**

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

> **Stock Verification:** Call `GET http://localhost:8081/products/1` in `product-service`. Notice that stock decreased from `10` to `8`.

---

### 2. Insufficient Stock (400 Bad Request)

Try to order more items than currently available in `product-service` (e.g. 999 items).

**Request:**
`POST http://localhost:8082/orders`

```json
{
  "productId": 1,
  "quantity": 999
}
```

**Response from `order-service`:**
`HTTP 400 Bad Request`

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot place order: Insufficient stock for product ID 1",
  "path": "/orders"
}
```

---

### 3. Place an Order for a Non-Existent Product (404 Not Found)

Try to order product ID `999` which does not exist in `product-service`.

**Request:**
`POST http://localhost:8082/orders`

```json
{
  "productId": 999,
  "quantity": 1
}
```

**Response from `order-service`:**
`HTTP 404 Not Found`

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 999 not found in product-service!",
  "path": "/orders"
}
```

---

## Configuration & Environment Variables

| Variable | Description | Default Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/orderdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `PRODUCT_SERVICE_URL` | Base URL for `product-service` | `http://localhost:8081` |
