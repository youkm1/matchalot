# 배포 단계별 가이드

## ✅ 완료된 작업
1. nginx 이미지 빌드 및 Docker Hub 푸시 완료
   - `youkm1/matchalot-vm-nginx:latest`

## 🔄 다음 단계

### 1. 백엔드 이미지 빌드를 위한 커밋
```bash
# workflow_dispatch 추가 커밋
git add .github/workflows/backend-deploy.yml
git commit -m "Add manual trigger to backend deploy workflow"
git push
```

### 2. GitHub Actions에서 백엔드 빌드 (수동 실행)
1. GitHub repository → Actions 탭
2. "Build and Deploy" workflow 선택
3. "Run workflow" 버튼 클릭
4. main 브랜치에서 실행

### 3. 빌드 상태 확인
- Gradle 빌드 (3-5분)
- Docker 이미지 빌드 및 푸시 (2-3분)
- 배포 및 헬스체크 (2-3분)

## 🐛 트러블슈팅

### 만약 빌드 실패 시:
1. **Gradle 빌드 실패**: 테스트 제외 확인
2. **Docker Hub 푸시 실패**: DOCKER_TOKEN 확인
3. **헬스체크 실패**: 환경변수 및 DB 연결 확인

### 현재 상태:
- ✅ nginx 이미지: Docker Hub에 업로드 완료
- ⏳ 백엔드 이미지: 빌드 대기 중
- 🔧 배포 스크립트: 새로운 방식으로 업데이트 완료

## 예상 결과:
- 서버에서 직접 빌드하지 않고 이미지만 pull
- 빌드 시간 15분 → 3-5분으로 단축
- 서버 메모리 부담 감소