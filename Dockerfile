FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# Strip CR so a Windows checkout (CRLF) still runs on Alpine (exit 127 = /bin/sh\r).
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/courier-tracking.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
