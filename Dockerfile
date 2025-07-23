FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /workspace/app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon --no-build-cache
COPY src src
RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
