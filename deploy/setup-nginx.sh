#!/bin/bash

echo "=== Nginx 설정 시작 ==="

# nginx 네트워크 확인 및 생성
if ! docker network ls | grep -q "matchalot-network"; then
  echo "Creating matchalot-network..."
  docker network create matchalot-network
fi

# 기존 nginx 컨테이너 정리
docker stop matchalot-nginx 2>/dev/null || true
docker rm matchalot-nginx 2>/dev/null || true

# nginx 템플릿에서 실제 설정 파일 생성
cp nginx-template.conf nginx.conf

# nginx 컨테이너 실행
docker run -d \
  --name matchalot-nginx \
  --network matchalot-network \
  -p 80:80 \
  -p 443:443 \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf \
  -v /etc/letsencrypt:/etc/letsencrypt:ro \
  -v /var/www/certbot:/var/www/certbot \
  --restart unless-stopped \
  nginx:alpine

echo "=== Nginx 설정 완료 ==="
docker ps --filter "name=matchalot-nginx"