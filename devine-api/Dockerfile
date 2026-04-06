FROM gradle:8.12-jdk21 AS build

WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY devine-core/ devine-core/
COPY devine-api/ devine-api/

RUN ./gradlew :devine-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl wget
COPY --from=build /app/devine-api/build/libs/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
