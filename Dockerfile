FROM eclipse-temurin:17-jdk-alpine AS build


WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 실행 환경
FROM eclipse-temurin:17-jre-alpine


WORKDIR /app

# Spring Boot JAR 복사
COPY --from=build /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
