# Multi-stage build for order-service

# Stage 1: Build stage using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build executable Spring Boot jar
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime environment using slim JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/order-service-*.jar app.jar

# Expose order-service port
EXPOSE 8082

# Set entrypoint to run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
