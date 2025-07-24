# 빌드 스테이지는 JDK 
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

# 네트워크 설정 및 필요 패키지 설치
RUN apk add --no-cache curl wget

# Gradle Wrapper와 설정 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./


RUN chmod +x ./gradlew

# 네트워크 타임아웃을 늘림
RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=600000 \
    -Dorg.gradle.internal.http.socketTimeout=600000 \
    --stacktrace || \
    (echo "재시도..." && ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=600000 \
    -Dorg.gradle.internal.http.socketTimeout=600000)

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# 실행 스테이지 - JRE 사용
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
