@echo off
setlocal enabledelayedexpansion
title Microservices Launcher

echo ===================================================
echo   Starting Microservices with Spring Boot Run
echo ===================================================

set ROOT_DIR=%~dp0

echo [1/2] Launching Product Service on port 8081...
start "Product Service (Port 8081)" cmd /k "cd /d "%ROOT_DIR%product-service" && title Product Service (8081) && echo Starting Product Service... && mvn spring-boot:run"

timeout /t 2 /nobreak >nul

echo [2/2] Launching Order Service on port 8082...
start "Order Service (Port 8082)" cmd /k "cd /d "%ROOT_DIR%order-service" && title Order Service (8082) && echo Starting Order Service... && mvn spring-boot:run"

echo.
echo ===================================================
echo Both microservices are running in dedicated windows!
echo - Product Service: http://localhost:8081 (Swagger: http://localhost:8081/swagger-ui.html)
echo - Order Service:   http://localhost:8082 (Swagger: http://localhost:8082/swagger-ui.html)
echo ===================================================
echo You can close the respective terminal window to stop a service.
echo.
pause
