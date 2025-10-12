#!/bin/bash

# Production 환경 설정 스크립트

echo "=== Production 환경 설정 ==="

# .env 파일이 없으면 생성
if [ ! -f .env ]; then
    echo ".env 파일이 없습니다. .env.example을 복사합니다."
    cp ../.env.example .env
    echo "✅ .env 파일이 생성되었습니다. 환경변수를 설정해주세요:"
    echo ""
    echo "필수 설정 항목:"
    echo "  - DB_HOST, DB_USERNAME, DB_PASSWORD"
    echo "  - GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET"
    echo "  - JWT_SECRET (보안을 위해 복잡한 키 사용)"
    echo "  - ADMIN_EMAIL"
    echo "  - MAIL_USERNAME, MAIL_PASSWORD"
    echo ""
    echo "Gmail 앱 비밀번호 생성:"
    echo "  1. https://myaccount.google.com/security 접속"
    echo "  2. 2단계 인증 활성화"
    echo "  3. 앱 비밀번호 생성"
    echo "  4. 16자리 비밀번호를 MAIL_PASSWORD에 입력 (공백 제거)"
    exit 1
fi

# 환경변수 확인
echo "환경변수 확인 중..."
required_vars=("DB_HOST" "DB_USERNAME" "DB_PASSWORD" "GOOGLE_CLIENT_ID" "GOOGLE_CLIENT_SECRET" "JWT_SECRET" "MAIL_USERNAME" "MAIL_PASSWORD")

missing_vars=()
for var in "${required_vars[@]}"; do
    if grep -q "^${var}=$" .env || ! grep -q "^${var}=" .env; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -ne 0 ]; then
    echo "❌ 다음 환경변수가 설정되지 않았습니다:"
    printf ' - %s\n' "${missing_vars[@]}"
    exit 1
fi

echo "✅ 모든 환경변수가 설정되었습니다."

# Docker 이미지 빌드
echo "Docker 이미지 빌드 중..."
cd ..
docker build -t matchalot:latest .

# Docker Compose 실행
echo "애플리케이션 시작 중..."
cd deploy
docker-compose up -d

echo "✅ 배포 완료!"
echo ""
echo "상태 확인:"
echo "  docker-compose ps"
echo "  docker-compose logs -f app"
echo ""
echo "이메일 테스트:"
echo "  curl -X POST http://localhost:8080/api/v1/test/email"