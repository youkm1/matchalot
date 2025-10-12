# GitHub Secrets 설정 가이드

## 📝 GitHub Secrets 설정 방법

### 1. GitHub Repository 설정
1. Repository 페이지 → **Settings** 탭
2. 왼쪽 메뉴에서 **Secrets and variables** → **Actions**
3. **New repository secret** 클릭

### 2. 필요한 Secrets 추가

다음 Secrets를 하나씩 추가:

| Secret Name | 설명 | 예시 값 |
|------------|------|---------|
| `DB_HOST` | 데이터베이스 호스트 | `your-rds-endpoint.amazonaws.com` |
| `DB_PORT` | DB 포트 | `5432` |
| `DB_NAME` | DB 이름 | `matchalot` |
| `DB_USERNAME` | DB 사용자명 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | `your-db-password` |
| `GOOGLE_CLIENT_ID` | Google OAuth ID | `283628099860-xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Secret | `GOCSPX-xxx` |
| `JWT_SECRET` | JWT 암호화 키 | `your-secret-key-min-32-chars` |
| `ADMIN_EMAIL` | 관리자 이메일 | `youkm0806@sookmyung.ac.kr` |
| `MAIL_USERNAME` | Gmail 계정 | `dudghkrkwhgek@gmail.com` |
| `MAIL_PASSWORD` | Gmail 앱 비밀번호 (공백 제거) | `mwbumeieoonkrdba` |

### 3. GitHub Actions에서 사용

```yaml
# .github/workflows/deploy.yml
env:
  MAIL_USERNAME: ${{ secrets.MAIL_USERNAME }}
  MAIL_PASSWORD: ${{ secrets.MAIL_PASSWORD }}
```

### 4. 로컬 개발 환경

로컬에서는 `.env` 파일 사용:
```bash
# .env (Git에 커밋하지 않음!)
MAIL_USERNAME=dudghkrkwhgek@gmail.com
MAIL_PASSWORD=mwbumeieoonkrdba
```

### 5. Docker 배포 시

```bash
# GitHub Actions에서 자동으로 환경변수 주입
docker run -d \
  -e DB_HOST=${{ secrets.DB_HOST }} \
  -e MAIL_USERNAME=${{ secrets.MAIL_USERNAME }} \
  -e MAIL_PASSWORD=${{ secrets.MAIL_PASSWORD }} \
  matchalot:latest
```

## 🔒 보안 체크리스트

- [ ] `.env` 파일이 `.gitignore`에 포함되어 있는지 확인
- [ ] 실제 비밀번호가 코드에 하드코딩되어 있지 않은지 확인
- [ ] GitHub Secrets가 모두 설정되었는지 확인
- [ ] Gmail 2단계 인증 활성화 확인
- [ ] Gmail 앱 비밀번호 생성 확인

## 📧 Gmail 앱 비밀번호 생성

1. [Google 계정 보안](https://myaccount.google.com/security) 접속
2. **2단계 인증** 활성화
3. **앱 비밀번호** 클릭
4. 앱 선택: **메일**
5. 기기 선택: **기타** → "MatchALot" 입력
6. 생성된 16자리 비밀번호 복사 (공백 제거)
7. `MAIL_PASSWORD`에 설정

## 🚀 배포 트리거

main 브랜치에 push하면 자동 배포:
```bash
git add .
git commit -m "Update configuration"
git push origin main
# → GitHub Actions가 자동으로 배포 시작
```

## ⚠️ 주의사항

1. **절대 Secrets를 로그에 출력하지 마세요**
   ```yaml
   # 잘못된 예
   - run: echo ${{ secrets.MAIL_PASSWORD }}  # ❌
   
   # 올바른 예  
   - run: echo "Deploying with secrets..."  # ✅
   ```

2. **Pull Request에서는 Secrets 접근 불가**
   - 보안상 fork된 repo의 PR은 Secrets 접근 제한
   - main 브랜치에 merge 후 배포

3. **Secrets 로테이션**
   - 정기적으로 비밀번호 변경
   - 특히 팀원 변경 시 즉시 변경