@echo off
setlocal
title Microservices Launcher (Docker Compose)

echo ===================================================
echo   Starting Microservices Stack with Docker Compose
echo ===================================================
echo - discovery-server  (Eureka: http://localhost:8761)
echo - api-gateway       (Public Entry: http://localhost:8080)
echo - product-service   (2 Replicas, Load-Balanced)
echo - order-service     (Load-Balanced)
echo - product-db        (PostgreSQL)
echo - order-db          (PostgreSQL)
echo ===================================================
echo.

docker compose up --build
