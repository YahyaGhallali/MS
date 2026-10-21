# Order Service (Spring Boot)

Microservice for managing orders, running on **port 8082** with its own in-memory H2 database.

---

## 📁 Project Structure

```text
order-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/orderservice/
        │   ├── Order.java                    # Entity (id, productId, quantity, unitPrice, totalPrice, status)
        │   ├── OrderRepository.java          # JPA Repository for orders
        │   ├── OrderController.java          # REST Controller with POST /orders using RestClient
        │   ├── ProductDTO.java               # DTO to map response from product-service
        │   ├── ProductNotFoundException.java # Custom 404 Exception
        │   └── OrderServiceApplication.java   # Main class & RestClient bean
        └── resources/
            └── application.properties        # Port (8082) & H2 (jdbc:h2:mem:orderdb)
```

---

## 🚀 How to Run Both Services

Open **two separate terminals**:

### Terminal 1: Start `product-service` (Port 8081)
```bash
cd c:\Users\yahya\Desktop\MS1
mvn spring-boot:run
```

### Terminal 2: Start `order-service` (Port 8082)
```bash
cd c:\Users\yahya\Desktop\MS1\order-service
mvn spring-boot:run
```

---

## 🌐 API Endpoints

| HTTP Method | URL | Description |
|---|---|---|
| **POST** | `http://localhost:8082/orders` | Create an order (fetches product price from port 8081) |
| **GET** | `http://localhost:8082/orders` | Get all orders |
| **GET** | `http://localhost:8082/orders/{id}` | Get order by ID |

---

## 🧪 Testing Scenarios

### 1. Place an Order for an Existing Product (Success - 200 OK)
In `product-service`, product ID `1` is the **Laptop** ($999.99).

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

---

### 2. Place an Order for a Non-Existent Product (404 Not Found)
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
  "timestamp": "2026-10-20T...",
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 999 not found in product-service!",
  "path": "/orders"
}
```

---

## 🗄️ H2 Database Console

- **URL:** `http://localhost:8082/h2-console`
- **JDBC URL:** `jdbc:h2:mem:orderdb`
- **User Name:** `sa`
- **Password:** *(leave empty)*
