# Dùng build stage để image runtime chỉ chứa JRE và artifact đã biên dịch.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Chạy ứng dụng bằng user không có quyền root trong container.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /build/target/todo-app-0.1.0.jar /app/todo-app.jar
RUN mkdir -p /data/uploads && chown -R appuser:appuser /app /data
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/todo-app.jar"]
