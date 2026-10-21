# PowerShell script to run the microservices stack via Docker Compose
$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting Microservices with Docker Compose      " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Services:" -ForegroundColor Yellow
Write-Host "  - discovery-server : http://localhost:8761 (Eureka Dashboard)" -ForegroundColor Cyan
Write-Host "  - api-gateway      : http://localhost:8080 (Public API Entry)" -ForegroundColor Cyan
Write-Host "  - kafka            : localhost:9092 (Apache Kafka Broker)" -ForegroundColor Cyan
Write-Host "  - product-service  : 2 Replicas (Registered in Eureka)" -ForegroundColor Cyan
Write-Host "  - order-service    : 1 Replica (Registered in Eureka)" -ForegroundColor Cyan
Write-Host "  - databases        : productdb, orderdb (PostgreSQL)" -ForegroundColor Cyan
Write-Host "`nPress Ctrl+C to stop all services.`n" -ForegroundColor DarkGray

docker compose up --build
