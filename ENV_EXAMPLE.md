# 환경 변수 설정 가이드

## .env 파일 생성

프로젝트 루트에 `.env` 파일을 생성하고 아래 내용을 추가하세요:

```env
# JWT 설정
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-required-for-production
JWT_EXPIRATION=86400000

# 카카오 OAuth2 설정
KAKAO_REST_API_KEY=your-kakao-rest-api-key
KAKAO_CLIENT_SECRET=your-kakao-client-secret
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback
FRONTEND_CALLBACK_URL=http://localhost:3000/dashboard

# Docker 환경 - 서비스 URL (컨테이너 이름 사용)
AUTH_SERVICE_URL=http://authservice:8081
USER_SERVICE_URL=http://userservice:8082

# 로컬 개발 환경 - 서비스 URL (localhost 사용)
# AUTH_SERVICE_URL=http://localhost:8081
# USER_SERVICE_URL=http://localhost:8082
```

## 중요 사항

### Docker 환경
- `AUTH_SERVICE_URL=http://authservice:8081` (컨테이너 이름)
- `USER_SERVICE_URL=http://userservice:8082` (컨테이너 이름)

### 로컬 개발 환경 (Gradle bootRun)
- `AUTH_SERVICE_URL=http://localhost:8081` (localhost)
- `USER_SERVICE_URL=http://localhost:8082` (localhost)

또는 환경 변수를 설정하지 않으면 기본값이 적용됩니다:
- Docker: `http://authservice:8081`, `http://userservice:8082`
- 로컬: PowerShell에서 환경 변수 설정 필요

## Docker Compose 재시작

```bash
# 컨테이너 중지 및 제거
docker-compose down

# 이미지 다시 빌드 및 실행
docker-compose up --build -d

# 로그 확인
docker-compose logs -f gateway
```

## 로컬 개발 환경 실행

### PowerShell
```powershell
# 환경 변수 설정
$env:JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-required"
$env:KAKAO_REST_API_KEY="your-kakao-rest-api-key"
$env:KAKAO_CLIENT_SECRET="your-kakao-client-secret"
$env:KAKAO_REDIRECT_URI="http://localhost:8080/api/auth/kakao/callback"
$env:FRONTEND_CALLBACK_URL="http://localhost:3000/dashboard"
$env:AUTH_SERVICE_URL="http://localhost:8081"
$env:USER_SERVICE_URL="http://localhost:8082"

# Gateway 실행
cd gateway
.\gradlew bootRun

# 새 터미널에서 Auth Service 실행
cd services\authservice
.\gradlew bootRun
```

### Bash
```bash
# 환경 변수 설정
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-required"
export KAKAO_REST_API_KEY="your-kakao-rest-api-key"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
export KAKAO_REDIRECT_URI="http://localhost:8080/api/auth/kakao/callback"
export FRONTEND_CALLBACK_URL="http://localhost:3000/dashboard"
export AUTH_SERVICE_URL="http://localhost:8081"
export USER_SERVICE_URL="http://localhost:8082"

# Gateway 실행
cd gateway
./gradlew bootRun

# 새 터미널에서 Auth Service 실행
cd services/authservice
./gradlew bootRun
```

