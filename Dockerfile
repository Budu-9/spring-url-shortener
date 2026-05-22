FROM eclipse-temurin:26-jdk-noble AS builder
WORKDIR /build

# Copy the Maven wrapper execution files and configuration
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Give the Linux container execution permissions for the wrapper script
RUN chmod +x mvnw

# Download dependencies in batch mode using the wrapper
RUN ./mvnw dependency:resolve -B

# Copy source code and compile the application artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Extract Spring Boot layers for granular Docker caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination /build/extracted

# === Runtime Stage ===
# Use SAP Machine for verified Java 26 runtime support
FROM sapmachine:26-jre-ubuntu

# Create a system non-root user for strict container security
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# Copy the decoupled application layers from the builder stage
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Drop privileges to the non-root execution user
USER appuser

EXPOSE 8080

# Production-tuned JVM container flags
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]


