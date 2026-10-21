# Multi-stage build for discovery-server

# Stage 1: Build stage using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build executable Spring Boot jar (skipping test compilation & test plugins)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip=true

# Stage 2: Minimal runtime environment using slim JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/discovery-server-*.jar app.jar

# Expose Eureka port
EXPOSE 8761

# Set entrypoint to run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
