# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage - using JDK instead of JRE to include jcmd for profiling
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create directory for profiling data (before switching to non-root user)
RUN mkdir -p /app/profiling && chown spring:spring /app/profiling

# Switch to non-root user
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/validation/health || exit 1

# Run application
# JFR is available but not started automatically - use profile.sh to start recordings
# JFR can be enabled manually via: docker exec <container> jcmd 1 JFR.start ...
ENTRYPOINT ["java", "-Xms8g", "-Xmx8g", "-jar", "app.jar"]

