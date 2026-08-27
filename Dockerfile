FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Create secrets directory
RUN mkdir -p /app/config /etc/secrets

# Copy secrets (will be overridden by volume mount)
COPY config/secrets.yml /app/config/secrets.yml



EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.additional-location=optional:file:/app/config/", "--spring.config.additional-location=optional:file:/etc/secrets/"]