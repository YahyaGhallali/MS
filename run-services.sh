#!/bin/bash
# Bash script to run the microservices stack via Docker Compose
set -e

echo "==================================================="
echo "  Starting Microservices with Docker Compose      "
echo "==================================================="
echo "Services:"
echo "  - discovery-server : http://localhost:8761 (Eureka Dashboard)"
echo "  - api-gateway      : http://localhost:8080 (Public API Entry)"
echo "  - product-service  : 2 Replicas (Registered in Eureka)"
echo "  - order-service    : 1 Replica (Registered in Eureka)"
echo "  - databases        : productdb, orderdb (PostgreSQL)"
echo ""
echo "Press Ctrl+C to stop all services."
echo ""

docker compose up --build
