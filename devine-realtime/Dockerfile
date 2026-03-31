FROM gradle:8.12-jdk21 AS build

WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY devine-core/ devine-core/
COPY devine-realtime/ devine-realtime/

RUN ./gradlew :devine-realtime:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl wget
COPY --from=build /app/devine-realtime/build/libs/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
