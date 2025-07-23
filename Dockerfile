FROM eclipse-temurin:17-jre-alpine

# 필요한 패키지 설치 및 시간대 설정
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

WORKDIR /app

# 로컬에서 빌드된 JAR 파일 복사
# 사전 작업: ./gradlew clean build -x test
COPY build/libs/*.jar app.jar

# 애플리케이션 포트
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 옵션과 함께 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-Dspring.profiles.active=prod", \
    "-Duser.timezone=Asia/Seoul", \
    "-jar", "app.jar"]
