FROM nginx:alpine

# nginx 설정 파일 복사
COPY nginx-ssl.conf /etc/nginx/nginx.conf

# 필요한 디렉토리 생성
RUN mkdir -p /var/log/nginx

# 포트 노출
EXPOSE 80 443

# nginx 실행
CMD ["nginx", "-g", "daemon off;"]