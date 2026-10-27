# Runtime image — package the verified JAR from `mvn package` (same commit as staging validation).
# Build order: mvn -B package -DskipTests  &&  docker compose build
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r trustagro && useradd -r -g trustagro trustagro \
    && mkdir -p /app/uploads && chown -R trustagro:trustagro /app
WORKDIR /app
COPY target/trust-agro-backend-1.0.0.jar app.jar
USER trustagro
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -sf http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
