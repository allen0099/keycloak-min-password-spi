# Stage 1: Build the SPI JAR using Maven
FROM maven:3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the JAR
RUN mvn clean package

# Stage 2: Create the Keycloak image with the SPI
FROM quay.io/keycloak/keycloak:24.0.1

# Copy the JAR from the builder stage to the providers directory
COPY --from=builder /app/target/keycloak-min-password-age-spi-1.0.0-SNAPSHOT.jar /opt/keycloak/providers/

# Run the build command to register the provider
RUN /opt/keycloak/bin/kc.sh build

# Set the entrypoint (optional, but good practice if we want dev mode by default or specific args)
# We will handle runtime args in compose.yaml
