# 백엔드 아키텍처 상세 보고서

## 📋 목차
1. [시스템 개요](#시스템-개요)
2. [프로젝트 구조](#프로젝트-구조)
3. [기술 스택](#기술-스택)
4. [마이크로서비스 아키텍처](#마이크로서비스-아키텍처)
5. [Gateway 상세](#gateway-상세)
6. [Auth Service 상세](#auth-service-상세)
7. [User Service 상세](#user-service-상세)
8. [데이터 플로우](#데이터-플로우)
9. [보안 설정](#보안-설정)
10. [환경 변수](#환경-변수)
11. [API 명세](#api-명세)
12. [배포 전략](#배포-전략)

---

## 시스템 개요

### 아키텍처 패턴
```
┌─────────────────────────────────────────────────────────────────┐
│                    마이크로서비스 아키텍처                         │
│                  (Spring Cloud Gateway 기반)                     │
└─────────────────────────────────────────────────────────────────┘

                        ┌──────────────┐
                        │   Frontend   │
                        │ (Next.js)    │
                        │ Port: 3000   │
                        └──────┬───────┘
                               │
                               │ HTTP/REST
                               ▼
                        ┌──────────────┐
                        │   Gateway    │
                        │ (Spring      │
                        │  Cloud)      │
                        │ Port: 8080   │
                        └──────┬───────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                │ SimpleDiscoveryClient       │
                │ (Load Balancing)            │
                │                             │
        ┌───────▼───────┐             ┌───────▼───────┐
        │ Auth Service  │             │ User Service  │
        │ Port: 8081    │             │ Port: 8082    │
        │               │             │               │
        │ - OAuth2      │             │ - User CRUD   │
        │ - JWT         │             │ - Profile     │
        │ - Kakao API   │             │               │
        └───────────────┘             └───────────────┘
                │                             │
                │                             │
                ▼                             ▼
        ┌───────────────┐             ┌───────────────┐
        │  Kakao API    │             │   Database    │
        │  (External)   │             │   (Future)    │
        └───────────────┘             └───────────────┘
```

### 핵심 특징
- ✅ **유레카 없는 서비스 디스커버리**: SimpleDiscoveryClient 사용
- ✅ **로드 밸런싱**: Spring Cloud LoadBalancer
- ✅ **API Gateway 패턴**: 단일 진입점
- ✅ **JWT 기반 인증**: Stateless 인증
- ✅ **OAuth2 소셜 로그인**: 카카오 연동
- ✅ **반응형 프로그래밍**: WebFlux 기반
- ✅ **마이크로서비스**: 독립적 배포 가능

---

## 프로젝트 구조

### 디렉토리 구조
```
api.kanggyeonggu.store/
│
├── build.gradle                    # 루트 빌드 설정
├── settings.gradle                 # 멀티모듈 설정
├── docker-compose.yaml            # Docker Compose 설정
│
├── gateway/                        # API Gateway (포트 8080)
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/
│       ├── java/store/kanggyoenggu/api/
│       │   ├── ApiApplication.java          # Gateway 메인 클래스
│       │   └── config/
│       │       ├── SecurityConfig.java       # Security 설정
│       │       └── SwaggerConfig.java        # Swagger 설정
│       └── resources/
│           └── application.yaml              # Gateway 설정
│
├── services/                       # 마이크로서비스들
│   ├── authservice/               # 인증 서비스 (포트 8081)
│   │   ├── build.gradle
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/store/kanggyoenggu/
│   │       │   ├── api/
│   │       │   │   └── ApiApplication.java   # Auth Service 메인
│   │       │   └── kakao/
│   │       │       ├── KakaoController.java  # 카카오 API 컨트롤러
│   │       │       ├── KakaoService.java     # 카카오 API 서비스
│   │       │       └── JwtService.java       # JWT 토큰 서비스
│   │       └── resources/
│   │           └── application.yaml          # Auth Service 설정
│   │
│   └── userservice/               # 사용자 서비스 (포트 8082)
│       ├── build.gradle
│       ├── Dockerfile
│       └── src/main/
│           ├── java/store/kanggyoenggu/api/
│           │   └── ApiApplication.java       # User Service 메인
│           └── resources/
│               └── application.yaml          # User Service 설정
│
└── common/                         # 공통 모듈 (삭제됨 - 필요시 재생성)
    └── (JWT, Exception, DTO 등)
```

---

## 기술 스택

### 프레임워크 & 라이브러리

#### 공통
```yaml
언어: Java 21
빌드 도구: Gradle 9.2.1
프레임워크: Spring Boot 3.5.7
Spring Cloud: 2025.0.0
```

#### Gateway
```gradle
dependencies {
  // Core
  - spring-cloud-starter-gateway-server-webflux
  - spring-boot-starter-actuator
  - spring-boot-starter-oauth2-client
  - spring-boot-starter-security
  - spring-boot-starter-webflux
  
  // Service Discovery & Load Balancing
  - spring-cloud-starter-loadbalancer (SimpleDiscoveryClient)
  
  // JWT
  - jjwt-api:0.12.3
  - jjwt-impl:0.12.3
  - jjwt-jackson:0.12.3
  
  // Documentation
  - springdoc-openapi-starter-webflux-ui:2.3.0
  
  // Development
  - spring-boot-devtools
}
```

#### Auth Service
```gradle
dependencies {
  // Core
  - spring-boot-starter-web
  - spring-boot-starter-actuator
  - spring-boot-starter-webflux (WebClient, Reactor)
  
  // JWT
  - jjwt-api:0.12.3
  - jjwt-impl:0.12.3
  - jjwt-jackson:0.12.3
  
  // Utilities
  - lombok
  
  // Development
  - spring-boot-devtools
  
  // Testing
  - spring-boot-starter-test
  - junit-platform-launcher
}
```

#### User Service
```gradle
dependencies {
  // Core
  - spring-boot-starter-web
  - spring-boot-starter-actuator
  
  // Utilities
  - lombok
  
  // Development
  - spring-boot-devtools
  
  // Testing
  - spring-boot-starter-test
  - junit-platform-launcher
}
```

---

## 마이크로서비스 아키텍처

### 서비스 디스커버리 (SimpleDiscoveryClient)

#### 특징
- ✅ **Eureka 불필요**: 정적 서버 리스트 사용
- ✅ **간단한 설정**: YAML 파일로 관리
- ✅ **로드 밸런싱**: Round Robin 전략
- ✅ **개발 환경 최적화**: 추가 서버 없이 사용 가능

#### 설정 (Gateway application.yaml)
```yaml
spring:
  cloud:
    discovery:
      client:
        simple:
          instances:
            # Auth Service 인스턴스
            auth-service:
              - uri: http://localhost:8081
                instance-id: auth-service-1
                metadata:
                  version: v1
                  zone: local
            
            # User Service 인스턴스
            user-service:
              - uri: http://localhost:8082
                instance-id: user-service-1
                metadata:
                  version: v1
                  zone: local
    
    # LoadBalancer 설정
    loadbalancer:
      enabled: true
      configurations: simple
```

#### 스케일 아웃 예시
```yaml
# 여러 인스턴스 추가 가능
auth-service:
  - uri: http://localhost:8081
    instance-id: auth-service-1
  - uri: http://localhost:8091
    instance-id: auth-service-2
  - uri: http://localhost:8092
    instance-id: auth-service-3
```

---

## Gateway 상세

### 역할
1. **API Gateway**: 모든 요청의 단일 진입점
2. **라우팅**: 요청을 적절한 마이크로서비스로 전달
3. **로드 밸런싱**: SimpleDiscoveryClient를 통한 부하 분산
4. **CORS 처리**: 프론트엔드 요청 허용
5. **문서화**: Swagger UI 제공

### 라우팅 설정

#### 파일: `gateway/src/main/resources/application.yaml`
```yaml
spring:
  cloud:
    gateway:
      routes:
        # OAuth2 카카오 콜백 라우팅 (Gateway 자체 처리)
        - id: oauth2-kakao-callback
          uri: http://localhost:8080
          predicates:
            - Path=/oauth2/kakao/callback
          filters:
            - RewritePath=/oauth2/kakao/callback, /login/oauth2/code/kakao
        
        # Auth Service 라우팅 (로드밸런싱)
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/kakao/**
        
        # User Service 라우팅 (향후 확장)
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
```

#### 라우팅 규칙 설명
```
┌─────────────────────────────────────────────────────────────────┐
│                       라우팅 규칙                                  │
└─────────────────────────────────────────────────────────────────┘

요청 URL                                    → 라우팅 대상
─────────────────────────────────────────────────────────────────
/api/auth/kakao/login                      → lb://auth-service
/api/auth/kakao/callback?code=xxx          → lb://auth-service
/api/auth/kakao/user                       → lb://auth-service
/api/auth/kakao/logout                     → lb://auth-service

/api/user/**                               → lb://user-service

/docs                                      → Gateway (Swagger UI)
/v3/api-docs/**                            → Gateway (OpenAPI)

/oauth2/kakao/callback                     → Gateway (OAuth2 처리)

lb:// = LoadBalancer URI (SimpleDiscoveryClient)
```

### CORS 설정

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"      # Next.js 프론트엔드
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600
```

### Security 설정

#### 파일: `gateway/src/main/java/store/kanggyoenggu/api/config/SecurityConfig.java`
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()); // 모든 요청 허용
        
        return http.build();
    }
}
```

**설계 철학**:
- Gateway는 단순히 라우팅만 담당
- 인증/인가는 각 마이크로서비스에서 처리
- Stateless 아키텍처

### Swagger 설정

#### 파일: `gateway/src/main/java/store/kanggyoenggu/api/config/SwaggerConfig.java`
```java
@Configuration
public class SwaggerConfig {
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}
```

**접근 URL**: `http://localhost:8080/docs`

---

## Auth Service 상세

### 역할
1. **카카오 OAuth2 인증**: 카카오 로그인 처리
2. **JWT 토큰 발급**: 사용자 인증 토큰 생성
3. **사용자 정보 조회**: JWT 기반 사용자 정보 제공
4. **로그아웃 처리**: 세션 종료

### 서비스 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                   Auth Service 구조                               │
└─────────────────────────────────────────────────────────────────┘

KakaoController
    ↓ (사용)
    ├── KakaoService  → 카카오 API 호출
    │   ├── getAccessToken()   # 인가 코드 → 액세스 토큰
    │   ├── getUserInfo()      # 액세스 토큰 → 사용자 정보
    │   ├── logout()           # 카카오 로그아웃
    │   └── unlink()           # 카카오 연결 끊기
    │
    └── JwtService    → JWT 토큰 관리
        ├── generateToken()    # JWT 생성
        ├── parseToken()       # JWT 파싱
        └── validateToken()    # JWT 검증
```

### KakaoController

#### 파일: `services/authservice/src/main/java/store/kanggyoenggu/kakao/KakaoController.java`

```java
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoController {
    
    private final KakaoService kakaoService;
    private final JwtService jwtService;
    
    // 의존성 주입
    public KakaoController(KakaoService kakaoService, JwtService jwtService) {
        this.kakaoService = kakaoService;
        this.jwtService = jwtService;
    }
    
    /**
     * 카카오 로그인 URL 생성
     * GET /api/auth/kakao/login
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> kakaoLogin() {
        String kakaoAuthUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=code",
            kakaoAuthorizationUri,
            kakaoRestApiKey,
            URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "카카오 로그인 URL 생성 성공",
            "authUrl", kakaoAuthUrl
        ));
    }
    
    /**
     * 카카오 OAuth2 콜백 처리
     * GET /api/auth/kakao/callback?code=xxx
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> kakaoCallback(@RequestParam String code) {
        return kakaoService.getAccessToken(code)
            .flatMap(tokenResponse -> {
                String accessToken = (String) tokenResponse.get("access_token");
                return kakaoService.getUserInfo(accessToken);
            })
            .map(userInfo -> {
                // 카카오 사용자 정보 추출
                Long kakaoId = ((Number) userInfo.get("id")).longValue();
                Map<String, Object> profile = extractProfile(userInfo);
                String nickname = (String) profile.get("nickname");
                
                // JWT 토큰 생성
                String jwtToken = jwtService.generateToken(kakaoId, nickname);
                
                // 프론트엔드로 리다이렉트
                String redirectUrl = String.format(
                    "%s?token=%s",
                    frontendCallbackUrl,
                    URLEncoder.encode(jwtToken, StandardCharsets.UTF_8)
                );
                
                return ResponseEntity.<Void>status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
            })
            .onErrorResume(e -> {
                String errorUrl = String.format(
                    "%s?error=login_failed",
                    frontendCallbackUrl
                );
                
                return Mono.just(ResponseEntity.<Void>status(HttpStatus.FOUND)
                    .header("Location", errorUrl)
                    .build());
            });
    }
    
    /**
     * 사용자 정보 조회
     * GET /api/auth/kakao/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "토큰이 없습니다."));
        }
        
        String token = authorization.substring(7);
        
        try {
            Map<String, Object> claims = jwtService.parseToken(token);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "사용자 정보 조회 성공",
                "user", Map.of(
                    "id", claims.get("sub"),
                    "kakaoId", claims.get("kakaoId"),
                    "nickname", claims.get("nickname")
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "토큰이 유효하지 않습니다."));
        }
    }
    
    /**
     * 로그아웃
     * POST /api/auth/kakao/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "로그아웃 성공"
        ));
    }
}
```

### KakaoService

#### 파일: `services/authservice/src/main/java/store/kanggyoenggu/kakao/KakaoService.java`

```java
@Service
public class KakaoService {
    
    private final WebClient webClient;
    
    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;
    
    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;
    
    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;
    
    public KakaoService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * 인가 코드로 액세스 토큰 요청
     */
    public Mono<Map<String, Object>> getAccessToken(String authorizationCode) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoRestApiKey);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", authorizationCode);
        
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            body.add("client_secret", kakaoClientSecret);
        }
        
        return webClient.post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(body)
            .retrieve()
            .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
    
    /**
     * 액세스 토큰으로 사용자 정보 조회
     */
    public Mono<Map<String, Object>> getUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        
        return webClient.get()
            .uri(userInfoUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
    
    /**
     * 카카오 로그아웃
     */
    public Mono<Map<String, Object>> logout(String accessToken) {
        String logoutUrl = "https://kapi.kakao.com/v1/user/logout";
        
        return webClient.post()
            .uri(logoutUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
    
    /**
     * 카카오 연결 끊기 (회원 탈퇴)
     */
    public Mono<Map<String, Object>> unlink(String accessToken) {
        String unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";
        
        return webClient.post()
            .uri(unlinkUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
}
```

### JwtService

#### 파일: `services/authservice/src/main/java/store/kanggyoenggu/kakao/JwtService.java`

```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * JWT 토큰 생성
     */
    public String generateToken(Long kakaoId, String nickname) {
        SecretKey key = getSecretKey();
        
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .claim("kakaoId", kakaoId)
            .claim("nickname", nickname)
            .subject(kakaoId.toString())
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(key)
            .compact();
    }
    
    /**
     * JWT 토큰 파싱
     */
    public Map<String, Object> parseToken(String token) {
        SecretKey key = getSecretKey();
        
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims;
    }
    
    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Auth Service 설정

#### 파일: `services/authservice/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: auth-service

server:
  port: 8081

# 카카오 OAuth2 설정
kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}
  client-secret: ${KAKAO_CLIENT_SECRET:}
  redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:8080/api/auth/kakao/callback}
  authorization-uri: https://kauth.kakao.com/oauth/authorize
  token-uri: https://kauth.kakao.com/oauth/token
  user-info-uri: https://kapi.kakao.com/v2/user/me

# JWT 설정
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}  # 24시간

# 프론트엔드 설정
frontend:
  callback-url: ${FRONTEND_CALLBACK_URL:http://localhost:3000/dashboard}

# Actuator 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

# 로깅
logging:
  level:
    root: INFO
    store.kanggyoenggu: DEBUG
```

---

## User Service 상세

### 역할 (향후 확장)
1. **사용자 프로필 관리**
2. **사용자 CRUD 작업**
3. **사용자 설정 관리**

### 현재 상태
- 기본 구조만 존재
- 향후 확장 예정

### User Service 설정

#### 파일: `services/userservice/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: user-service

server:
  port: 8082

# Actuator 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

# 로깅
logging:
  level:
    root: INFO
    store.kanggyoenggu: DEBUG
```

---

## 데이터 플로우

### 카카오 로그인 전체 플로우

```
┌─────────────────────────────────────────────────────────────────┐
│                  카카오 로그인 데이터 플로우                        │
└─────────────────────────────────────────────────────────────────┘

1. [Frontend] 카카오 로그인 버튼 클릭
   ↓
   GET http://localhost:8080/api/auth/kakao/login

2. [Gateway] 라우팅
   ↓
   lb://auth-service (SimpleDiscoveryClient)
   ↓
   http://localhost:8081/api/auth/kakao/login

3. [Auth Service] 카카오 로그인 URL 생성
   ↓
   Response:
   {
     "success": true,
     "authUrl": "https://kauth.kakao.com/oauth/authorize?..."
   }

4. [Frontend] 카카오 로그인 페이지로 리다이렉트
   ↓
   window.location.href = authUrl

5. [Kakao] 사용자 인증
   ↓
   사용자가 카카오 계정으로 로그인

6. [Kakao] 콜백
   ↓
   GET http://localhost:8080/api/auth/kakao/callback?code=인가코드

7. [Gateway] 라우팅
   ↓
   lb://auth-service
   ↓
   http://localhost:8081/api/auth/kakao/callback?code=인가코드

8. [Auth Service] 콜백 처리
   ↓
   ① KakaoService.getAccessToken(code)
      POST https://kauth.kakao.com/oauth/token
      Body: grant_type=authorization_code&client_id=xxx&code=xxx
      ↓
      Response: { "access_token": "xxx", ... }
   
   ② KakaoService.getUserInfo(accessToken)
      GET https://kapi.kakao.com/v2/user/me
      Header: Authorization: Bearer {accessToken}
      ↓
      Response: { "id": 123456789, "kakao_account": {...}, ... }
   
   ③ JwtService.generateToken(kakaoId, nickname)
      ↓
      JWT: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   
   ④ 프론트엔드로 리다이렉트
      ↓
      HTTP 302 Redirect
      Location: http://localhost:3000/dashboard?token=JWT토큰

9. [Frontend] 토큰 처리
   ↓
   ① URL에서 token 추출
   ② localStorage에 저장
   ③ URL 정리 (보안)
   ④ 사용자 정보 조회
      ↓
      GET http://localhost:8080/api/auth/kakao/user
      Header: Authorization: Bearer {token}

10. [Gateway] 라우팅
    ↓
    lb://auth-service
    ↓
    http://localhost:8081/api/auth/kakao/user

11. [Auth Service] 사용자 정보 반환
    ↓
    JwtService.parseToken(token)
    ↓
    Response:
    {
      "success": true,
      "user": {
        "id": "123456789",
        "kakaoId": 123456789,
        "nickname": "홍길동"
      }
    }

12. [Frontend] 대시보드에 사용자 정보 표시
    ↓
    "안녕하세요, 홍길동님!"
```

---

## 보안 설정

### 1. JWT 보안

#### JWT 구조
```json
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "123456789",          // 주체 (카카오 ID)
  "kakaoId": 123456789,        // 카카오 ID
  "nickname": "홍길동",         // 닉네임
  "iat": 1641234567,           // 발급 시간
  "exp": 1641320967            // 만료 시간 (24시간 후)
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

#### JWT Secret 요구사항
- 최소 32자 이상
- 랜덤하고 예측 불가능
- 환경 변수로 관리
- 절대 코드에 하드코딩 금지

### 2. CORS 보안

```yaml
allowedOrigins:
  - "http://localhost:3000"           # 개발 환경
  # - "https://yourdomain.com"        # 프로덕션 환경

allowedMethods:
  - GET, POST, PUT, DELETE, OPTIONS   # 필요한 메서드만 허용

allowedHeaders:
  - "*"                               # 모든 헤더 허용 (필요시 제한)

allowCredentials: true                # 쿠키 전송 허용

maxAge: 3600                          # Preflight 캐시 1시간
```

### 3. OAuth2 보안

```yaml
# 카카오 OAuth2 설정
kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}           # 환경 변수로 관리
  client-secret: ${KAKAO_CLIENT_SECRET}         # 환경 변수로 관리
  redirect-uri: http://localhost:8080/api/auth/kakao/callback
```

**주의사항**:
- Redirect URI는 카카오 개발자 센터에 정확히 등록
- REST API 키와 Client Secret은 절대 노출 금지
- HTTPS 사용 (프로덕션 환경)

### 4. 보안 체크리스트

- ✅ JWT Secret 환경 변수로 관리
- ✅ JWT 만료 시간 설정 (24시간)
- ✅ CORS Origin 화이트리스트 관리
- ✅ CSRF 비활성화 (Stateless API)
- ✅ HTTPS 사용 (프로덕션)
- ✅ 민감 정보 로깅 금지
- ✅ 401 응답 시 자동 로그아웃
- ⚠️ Rate Limiting 필요 (추후 추가)
- ⚠️ API Key Rotation 정책 필요
- ⚠️ Refresh Token 구현 필요 (추후)

---

## 환경 변수

### Gateway & Auth Service

```bash
# JWT 설정
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-required
JWT_EXPIRATION=86400000  # 24시간 (밀리초)

# 카카오 OAuth2 설정
KAKAO_REST_API_KEY=your-kakao-rest-api-key
KAKAO_CLIENT_SECRET=your-kakao-client-secret
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback

# 프론트엔드 설정
FRONTEND_CALLBACK_URL=http://localhost:3000/dashboard
```

### PowerShell에서 설정

```powershell
# 환경 변수 설정
$env:JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-required"
$env:JWT_EXPIRATION="86400000"
$env:KAKAO_REST_API_KEY="your-kakao-rest-api-key"
$env:KAKAO_CLIENT_SECRET="your-kakao-client-secret"
$env:KAKAO_REDIRECT_URI="http://localhost:8080/api/auth/kakao/callback"
$env:FRONTEND_CALLBACK_URL="http://localhost:3000/dashboard"
```

### Bash에서 설정

```bash
# 환경 변수 설정
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-required"
export JWT_EXPIRATION="86400000"
export KAKAO_REST_API_KEY="your-kakao-rest-api-key"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
export KAKAO_REDIRECT_URI="http://localhost:8080/api/auth/kakao/callback"
export FRONTEND_CALLBACK_URL="http://localhost:3000/dashboard"
```

### .env 파일 (Docker Compose)

```env
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-required
JWT_EXPIRATION=86400000
KAKAO_REST_API_KEY=your-kakao-rest-api-key
KAKAO_CLIENT_SECRET=your-kakao-client-secret
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback
FRONTEND_CALLBACK_URL=http://localhost:3000/dashboard
```

---

## API 명세

### Auth Service API

#### 1. 카카오 로그인 URL 생성

```http
GET /api/auth/kakao/login
```

**Request**:
```
(없음)
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "카카오 로그인 URL 생성 성공",
  "authUrl": "https://kauth.kakao.com/oauth/authorize?client_id=xxx&redirect_uri=xxx&response_type=code"
}
```

---

#### 2. 카카오 OAuth2 콜백

```http
GET /api/auth/kakao/callback?code={인가코드}
```

**Request**:
```
Query Parameters:
  - code: 카카오에서 발급한 인가 코드
```

**Response** (302 Found):
```
Location: http://localhost:3000/dashboard?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Error Response** (302 Found):
```
Location: http://localhost:3000/dashboard?error=login_failed
```

---

#### 3. 사용자 정보 조회

```http
GET /api/auth/kakao/user
Authorization: Bearer {JWT 토큰}
```

**Request**:
```
Headers:
  - Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "user": {
    "id": "123456789",
    "kakaoId": 123456789,
    "nickname": "홍길동"
  }
}
```

**Error Response** (401 Unauthorized):
```json
{
  "success": false,
  "message": "토큰이 유효하지 않습니다."
}
```

---

#### 4. 로그아웃

```http
POST /api/auth/kakao/logout
Authorization: Bearer {JWT 토큰}
```

**Request**:
```
Headers:
  - Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "로그아웃 성공"
}
```

---

### Gateway API

#### Swagger UI
```
GET /docs
```

#### OpenAPI 문서
```
GET /v3/api-docs
GET /api-docs
```

#### Health Check
```
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## 배포 전략

### 로컬 개발 환경

#### 실행 순서

```bash
# 1. Gateway 실행
cd gateway
./gradlew bootRun

# 2. Auth Service 실행 (새 터미널)
cd services/authservice
./gradlew bootRun

# 3. User Service 실행 (새 터미널, 선택사항)
cd services/userservice
./gradlew bootRun
```

#### 포트 확인
```
Gateway:      http://localhost:8080
Auth Service: http://localhost:8081
User Service: http://localhost:8082
Frontend:     http://localhost:3000
```

---

### Docker Compose 배포

#### 파일: `docker-compose.yaml`

```yaml
version: '3.8'

services:
  gateway:
    build: ./gateway
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - KAKAO_REST_API_KEY=${KAKAO_REST_API_KEY}
      - KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
      - KAKAO_REDIRECT_URI=${KAKAO_REDIRECT_URI}
      - FRONTEND_CALLBACK_URL=${FRONTEND_CALLBACK_URL}
    depends_on:
      - auth-service
      - user-service
    networks:
      - microservices

  auth-service:
    build: ./services/authservice
    ports:
      - "8081:8081"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - KAKAO_REST_API_KEY=${KAKAO_REST_API_KEY}
      - KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
      - FRONTEND_CALLBACK_URL=${FRONTEND_CALLBACK_URL}
    networks:
      - microservices

  user-service:
    build: ./services/userservice
    ports:
      - "8082:8082"
    networks:
      - microservices

networks:
  microservices:
    driver: bridge
```

#### 실행

```bash
# .env 파일 생성
cat > .env << EOF
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-required
KAKAO_REST_API_KEY=your-kakao-rest-api-key
KAKAO_CLIENT_SECRET=your-kakao-client-secret
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback
FRONTEND_CALLBACK_URL=http://localhost:3000/dashboard
EOF

# Docker Compose 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 종료
docker-compose down
```

---

### Kubernetes 배포 (선택사항)

#### Deployment 예시

```yaml
# gateway-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
      - name: gateway
        image: kanggyoenggu/gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        - name: KAKAO_REST_API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: kakao-rest-api-key
---
apiVersion: v1
kind: Service
metadata:
  name: gateway
spec:
  selector:
    app: gateway
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: LoadBalancer
```

---

## 🎯 핵심 요약

### 아키텍처 특징
- ✅ **마이크로서비스**: Gateway + Auth Service + User Service
- ✅ **서비스 디스커버리**: SimpleDiscoveryClient (Eureka 불필요)
- ✅ **로드 밸런싱**: Spring Cloud LoadBalancer
- ✅ **API Gateway**: 단일 진입점, 라우팅, CORS
- ✅ **인증**: JWT + OAuth2 (Kakao)
- ✅ **반응형**: WebFlux, Reactor, Mono

### 기술 스택
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.5.7, Spring Cloud 2025.0.0
- **빌드**: Gradle 9.2.1
- **인증**: JWT (jjwt 0.12.3), OAuth2 (Kakao)
- **문서화**: Swagger/OpenAPI

### 서비스 구성
```
Gateway (8080)
  ↓ (라우팅)
  ├─ Auth Service (8081)
  │  ├─ KakaoController
  │  ├─ KakaoService
  │  └─ JwtService
  │
  └─ User Service (8082)
     └─ (향후 확장)
```

### API 엔드포인트
```
GET  /api/auth/kakao/login      → 카카오 로그인 URL 생성
GET  /api/auth/kakao/callback   → 카카오 콜백 처리
GET  /api/auth/kakao/user       → 사용자 정보 조회
POST /api/auth/kakao/logout     → 로그아웃

GET  /docs                      → Swagger UI
GET  /actuator/health           → Health Check
```

### 환경 변수
```
JWT_SECRET                 → JWT 서명 키
KAKAO_REST_API_KEY         → 카카오 REST API 키
KAKAO_CLIENT_SECRET        → 카카오 클라이언트 시크릿
KAKAO_REDIRECT_URI         → 카카오 리다이렉트 URI
FRONTEND_CALLBACK_URL      → 프론트엔드 콜백 URL
```

---

## 📚 참고 자료

### 공식 문서
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud LoadBalancer](https://spring.io/guides/gs/spring-cloud-loadbalancer/)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
- [JJWT](https://github.com/jwtk/jjwt)
- [카카오 로그인 REST API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)

### 추천 도구
- [jwt.io](https://jwt.io/) - JWT 디코더
- [Postman](https://www.postman.com/) - API 테스트
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) - 컨테이너 실행

---

**이 보고서는 현재 백엔드 시스템의 완전한 구조와 동작 방식을 설명합니다.** 🚀

