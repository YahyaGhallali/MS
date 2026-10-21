#!/bin/bash
# Bash script to run the microservices stack via Docker Compose
set -e

echo "==================================================="
echo "  Starting Microservices with Docker Compose      "
echo "==================================================="
echo "Services: product-db, order-db, product-service (8081), order-service (8082)"
echo "Press Ctrl+C to stop all services."
echo ""

docker compose up --build
