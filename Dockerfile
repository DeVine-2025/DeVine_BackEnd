FROM gradle:8.5-jdk17 AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./

RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
