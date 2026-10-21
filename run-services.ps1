# PowerShell script to run the microservices stack via Docker Compose
$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting Microservices with Docker Compose      " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Services: product-db, order-db, product-service (8081), order-service (8082)" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop all services.`n" -ForegroundColor DarkGray

docker compose up --build
