# Order Service (Spring Boot)

Microservice for managing orders, backed by PostgreSQL (`orderdb`), registered with **Eureka Discovery Server**, and accessible via **API Gateway (Port 8080)**.

---

## Architecture & Service Discovery

- **Service Registry**: Registers with Eureka under application name `order-service`.
- **Client-Side Load Balancing**: Communicates with `product-service` using Spring Cloud LoadBalancer via logical URI `http://product-service/...`.
- **Private Microservice**: In Docker Compose, `order-service` has no published host port and is accessed via `http://localhost:8080/orders`.

---

## Project Structure

```text
order-service/
├── Dockerfile                          # Multi-stage Docker build
├── .dockerignore                       # Docker ignore patterns
├── pom.xml                             # Maven build (Spring Boot 3.4.3, Spring Cloud 2024.0.0, Eureka Client, LoadBalancer)
└── src/
    ├── main/
    │   ├── java/com/example/orderservice/
    │   │   ├── Order.java                      # Entity
    │   │   ├── OrderRepository.java            # JPA Repository
    │   │   ├── OrderController.java            # REST Controller using http://product-service
    │   │   ├── ProductDTO.java                 # DTO
    │   │   ├── ProductNotFoundException.java   # Custom 404 Exception
    │   │   ├── InsufficientStockException.java # Custom 400 Exception
    │   │   ├── OrderProcessingException.java   # Custom 500 Exception for Saga compensation
    │   │   └── OrderServiceApplication.java    # @LoadBalanced RestClient.Builder configuration
    │   └── resources/
    │       └── application.properties          # Port 8082, Eureka client, PostgreSQL config
    └── test/                                   # Full unit and mock test suite
```

---

## How to Run

### Option A: Using Docker Compose (Recommended)

From the project root directory:

```bash
docker compose up --build
```

Access via the API Gateway at `http://localhost:8080/orders`.  
View registered instances at Eureka Dashboard: `http://localhost:8761`.

### Option B: Local Maven Execution

If you have PostgreSQL running locally on port 5432 and Eureka running on port 8761:

```bash
cd order-service
mvn spring-boot:run
```

---

## API Endpoints (via API Gateway)

| HTTP Method | Gateway URL | Description |
| --- | --- | --- |
| **POST** | `http://localhost:8080/orders` | Create an order (atomically deducts stock from load-balanced `product-service`) |
| **GET** | `http://localhost:8080/orders` | Get all orders |
| **GET** | `http://localhost:8080/orders/{id}` | Get order by ID |
