# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8069
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
