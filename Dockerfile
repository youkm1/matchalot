FROM eclipse-temurin:17-jre-alpine AS build
WORKDIR /workspace/app

COPY build.gradle settings.gradle ./

RUN gradle dependencies --no-daemon --no-build-cache \
    -Dorg.gradle.internal.http.connectionTimeout=300000 \
    -Dorg.gradle.internal.http.socketTimeout=300000 \
    || (echo "의존성 다운로드 재시도..." && \
        gradle dependencies --no-daemon --no-build-cache \
        -Dorg.gradle.internal.http.connectionTimeout=300000 \
        -Dorg.gradle.internal.http.socketTimeout=300000)
        
COPY src src

RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata


WORKDIR /app

COPY --from=build /workspace/app/build/libs/*.jar app.jar


EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-Dspring.profiles.active=prod", \
    "-Duser.timezone=Asia/Seoul", \
    "-jar", "app.jar"]
