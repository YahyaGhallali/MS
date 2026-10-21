# PowerShell script to run both microservices concurrently in separate windows
$ErrorActionPreference = "Stop"

$rootDir = $PSScriptRoot

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting Microservices with Spring Boot Run     " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

Write-Host "`n[1/2] Starting Product Service (Port 8081)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = 'Product Service (Port 8081)'; Set-Location '$rootDir\product-service'; Write-Host 'Starting Product Service on port 8081...' -ForegroundColor Green; mvn spring-boot:run"

Start-Sleep -Seconds 2

Write-Host "[2/2] Starting Order Service (Port 8082)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = 'Order Service (Port 8082)'; Set-Location '$rootDir\order-service'; Write-Host 'Starting Order Service on port 8082...' -ForegroundColor Green; mvn spring-boot:run"

Write-Host "`n===================================================" -ForegroundColor Green
Write-Host "Both microservices have been launched in separate terminal windows!" -ForegroundColor Green
Write-Host "  - Product Service: http://localhost:8081 (Swagger: http://localhost:8081/swagger-ui.html)" -ForegroundColor Cyan
Write-Host "  - Order Service:   http://localhost:8082 (Swagger: http://localhost:8082/swagger-ui.html)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Green
Write-Host "Close the individual PowerShell windows or press Ctrl+C inside them to stop the services.`n"
