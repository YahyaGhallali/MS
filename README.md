# MS1 Microservices Workspace: Service Discovery & API Gateway

This repository contains a full microservices ecosystem built with **Spring Boot 3.4.3**, **Spring Cloud 2024.0.0 (Moorgate)**, **Netflix Eureka**, and **Spring Cloud Gateway**, backed by PostgreSQL.

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
                    │ - Replica 1             │   │ - Port 8082 (Internal)  │
                    │ - Replica 2             │   │ - Client-Side           │
                    │ - Port 8081 (Internal)  │◄──│   LoadBalancer          │
                    │ - Attaches X-Instance-Id│   │ - http://product-service│
                    └───────────┬─────────────┘   └───────────┬─────────────┘
                                │                             │
                                ▼                             ▼
                    ┌─────────────────────────┐   ┌─────────────────────────┐
                    │ product-db (PostgreSQL) │   │ order-db (PostgreSQL)   │
                    └─────────────────────────┘   └─────────────────────────┘
```

### Key Architectural Concepts
1. **Dynamic Service Discovery**: Services register automatically with Eureka using their application name. Heartbeats, lease renewal, and health monitoring ensure dynamic registration without hardcoded IPs or ports.
2. **Client-Side Load Balancing**: `order-service` uses `@LoadBalanced RestClient.Builder` with Spring Cloud LoadBalancer to call `http://product-service/...`. Requests are automatically distributed round-robin across active `product-service` instances.
3. **Single Public Gateway (Reverse Proxy & Edge Concerns)**: Only `api-gateway` (port 8080) and `discovery-server` (port 8761) expose ports to the host machine. Downstream services are completely private within the Docker bridge network.
4. **Distributed Tracing & Correlation**: The Gateway automatically generates or propagates an `X-Correlation-Id` header for each request and logs request lifecycles.
5. **Instance Identification**: Each `product-service` replica logs its instance identifier (`HOSTNAME`) and attaches an `X-Instance-Id` header to responses, allowing easy verification of load distribution.

---

## Project Structure

```text
MS1/
├── pom.xml                               # Root parent POM with Spring Cloud 2024.0.0 BOM
├── docker-compose.yml                    # Multi-container orchestration (Eureka, Gateway, DBs, Services)
├── run-services.bat                      # Windows CMD launcher (Docker Compose)
├── run-services.ps1                      # PowerShell launcher (Docker Compose)
├── run-services.sh                       # Bash launcher (Docker Compose)
├── discovery-server/                     # Eureka Discovery Server (Port 8761)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
├── api-gateway/                          # Reactive Spring Cloud Gateway (Port 8080)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/apigateway/
│       │   ├── ApiGatewayApplication.java
│       │   └── LoggingAndCorrelationFilter.java
│       └── resources/application.yml
├── product-service/                      # Catalog Service (2 Replicas, Port 8081 Internal)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/productservice/
│       │   ├── ProductController.java
│       │   ├── InstanceIdFilter.java     # Attaches X-Instance-Id response header
│       │   └── ...
│       └── resources/application.properties
└── order-service/                        # Order Service (Port 8082 Internal)
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/example/orderservice/
        │   ├── OrderController.java      # Calls http://product-service
        │   ├── OrderServiceApplication.java # Configures @LoadBalanced RestClient
        │   └── ...
        └── resources/application.properties
```

---

## Port Summary

| Component | Port | Host Access | Description |
|---|---|---|---|
| **API Gateway** | `8080` | `http://localhost:8080` | **Sole public API entry point** |
| **Eureka Server** | `8761` | `http://localhost:8761` | **Eureka Discovery Dashboard & Registry** |
| **product-service** | `8081` | *Internal only* | Replicated (2 instances), load balanced |
| **order-service** | `8082` | *Internal only* | Calls `http://product-service` via LoadBalancer |
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

View live logs (including gateway correlation and instance routing):

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

### 2. Access Catalog via API Gateway
`GET http://localhost:8080/products`

Response:
```json
[
  { "id": 1, "name": "Laptop", "price": 999.99, "stock": 10 },
  { "id": 2, "name": "Mouse", "price": 29.99, "stock": 50 },
  { "id": 3, "name": "Keyboard", "price": 49.99, "stock": 25 }
]
```
Response Headers will include:
- `X-Correlation-Id: <generated-uuid>`
- `X-Instance-Id: <container-id-of-responding-replica>`

---

### 3. Place Orders and Verify Alternating Load Balancing
Place multiple orders through the Gateway:

`POST http://localhost:8080/orders`  
Headers: `Content-Type: application/json`  
Body:
```json
{
  "productId": 1,
  "quantity": 1
}
```

Observe in the container logs (`docker compose logs -f product-service`):
```text
product-service-1 | [product-service:d4a1b2c3] Deducting stock for product ID 1, quantity 1
product-service-2 | [product-service:e5f6a7b8] Deducting stock for product ID 1, quantity 1
```
Requests alternate round-robin between the two `product-service` replicas!

---

### 4. Correlation ID Tracing
Send a custom Correlation ID:
```bash
curl -i -H "X-Correlation-Id: trace-abc-123" http://localhost:8080/products
```
The Gateway logs the incoming request with that ID, passes it to the downstream service, and returns `X-Correlation-Id: trace-abc-123` in the HTTP response.

---

## Swagger Through the Gateway (Task 6 Analysis)

The OpenAPI specs for downstream services are reachable through the Gateway:
- **`product-service` OpenAPI Spec**: `http://localhost:8080/v3/api-docs/product-service`
- **`order-service` OpenAPI Spec**: `http://localhost:8080/v3/api-docs/order-service`

### Is Aggregating Swagger UI in the Gateway Worth the Effort?
- **Trade-off Analysis**:
  - **Pros**: Gives developers a single web page to discover and interact with all APIs.
  - **Cons & Complexities**:
    1. **Reactive vs. Servlet Mismatch**: Spring Cloud Gateway is built on Spring WebFlux (reactive Netty), whereas downstream services use Spring WebMVC (Tomcat). Hosting Swagger UI directly in the Gateway requires pulling in WebFlux Swagger UI (`springdoc-openapi-starter-webflux-ui`).
    2. **URL Rewrite & "Try It Out" Routing**: Downstream OpenAPI specs generate server URLs based on the service's internal port or relative path. For Swagger UI's "Try It Out" buttons to work through the Gateway without CORS or 404 errors, URL rewrites, OpenAPI server mutation filters, and `X-Forwarded-*` header forwarding must be heavily customized.
    3. **Operational Boundary**: An API Gateway in production is meant to be a lightweight, high-throughput edge router, reverse proxy, and rate limiter. Running developer documentation UIs inside the edge proxy mixes operational runtime concerns with developer tooling.
  - **Conclusion & Best Practice**: In production, routing the OpenAPI JSON specs (`/v3/api-docs/...`) through the Gateway (as implemented) is standard. Serving the visual Swagger UI is typically handled by a dedicated developer portal or API documentation service.

---

## Running Automated Tests

All 34 automated unit and integration tests across the 4 modules can be executed with:

```bash
mvn test
```
