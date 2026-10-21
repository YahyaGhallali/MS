# MS1 Microservices Workspace

This repository contains two independent Spring Boot microservices:

1. **`product-service`** (Runs on port `8081`, manages product catalog)
2. **`order-service`** (Runs on port `8082`, places orders and communicates with `product-service` via `RestClient`)

---

## 📁 Project Structure

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
        │   └── OrderServiceApplication.java
        └── resources/
            └── application.properties    # port 8082, jdbc:h2:mem:orderdb
```

---

## 🚀 How to Run

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

#### Terminal 2: Run Order Service

```bash
cd c:\Users\yahya\Desktop\MS1\order-service
mvn spring-boot:run
```

- Available at: `http://localhost:8082/orders`
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`
- H2 Console: `http://localhost:8082/h2-console` (JDBC: `jdbc:h2:mem:orderdb`)

---

## 📖 Swagger UI Interactive Documentation

Both microservices have Swagger UI enabled for testing APIs directly in your browser without needing curl or Postman:

| Service | Swagger UI URL | OpenAPI Spec |
|---|---|---|
| **`product-service`** (Port 8081) | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| **`order-service`** (Port 8082) | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |

> **Tip:** In the Swagger UI dashboard, click any endpoint and hit **"Try it out"** -> **"Execute"** to send live requests.

---

## 🌐 Endpoints & Testing

### 1. Check Pre-loaded Products (product-service)

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

### 3. Product Not Found (order-service)

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

### 4. Insufficient Stock (order-service)

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

