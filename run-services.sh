#!/bin/bash
# Bash script to run both microservices concurrently
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "==================================================="
echo "  Starting Microservices with Spring Boot Run     "
echo "==================================================="

echo "[1/2] Starting Product Service (Port 8081)..."
(cd "$DIR/product-service" && mvn spring-boot:run) &
PID_PRODUCT=$!

sleep 2

echo "[2/2] Starting Order Service (Port 8082)..."
(cd "$DIR/order-service" && mvn spring-boot:run) &
PID_ORDER=$!

echo ""
echo "==================================================="
echo "Both services are running in background!"
echo "  Product Service PID: $PID_PRODUCT (http://localhost:8081)"
echo "  Order Service PID:   $PID_ORDER (http://localhost:8082)"
echo "==================================================="
echo "Press Ctrl+C to terminate both microservices."

cleanup() {
    echo ""
    echo "Stopping microservices..."
    kill "$PID_PRODUCT" "$PID_ORDER" 2>/dev/null
    exit 0
}

trap cleanup SIGINT SIGTERM
wait
