# ===== Build Stage =====
FROM gradle:8-jdk21 AS build

WORKDIR /app
COPY backend/admin .

RUN chmod +x gradlew && \
    ./gradlew :backend:admin:bootJar --no-daemon

# ===== Runtime Stage =====
FROM eclipse-temurin:21

WORKDIR /app

COPY --from=build /app/backend/admin/build/libs/admin-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]