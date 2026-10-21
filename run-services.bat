@echo off
setlocal
title Microservices Launcher (Docker Compose)

echo ===================================================
echo   Starting Microservices Stack with Docker Compose
echo ===================================================
echo - product-db      (PostgreSQL)
echo - order-db        (PostgreSQL)
echo - product-service (Port 8081)
echo - order-service   (Port 8082)
echo ===================================================
echo.

docker compose up --build
