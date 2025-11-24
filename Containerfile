# Stage 1: Build the SPI JAR using Maven
FROM maven:3-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the JAR
RUN mvn clean package

# Stage 2: Create the Keycloak image with the SPI
FROM quay.io/keycloak/keycloak:26.4.5

# Copy the JAR from the builder stage to the providers directory
COPY --from=builder /app/target/keycloak-min-password-age-spi-*.jar /opt/keycloak/providers/

# Run the build command to register the provider
RUN /opt/keycloak/bin/kc.sh build

# Set the entrypoint (optional, but good practice if we want dev mode by default or specific args)
# We will handle runtime args in compose.yaml
