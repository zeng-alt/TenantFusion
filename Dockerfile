# ===== Build Stage =====
# syntax=docker/dockerfile:1.4
FROM gradle:8-jdk21 AS build

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :backend:admin:bootJar --no-daemon

# ===== Runtime Stage =====
FROM eclipse-temurin:21

WORKDIR /app

COPY --from=build /app/backend/admin/build/libs/admin-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]