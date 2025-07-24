FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app


COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 캐싱을 위한 별도 스텝
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

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
