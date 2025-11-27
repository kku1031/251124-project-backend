package store.kanggyoenggu.authservice.controller.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.kanggyoenggu.authservice.dto.*;
import store.kanggyoenggu.authservice.dto.oauth.GoogleTokenResponse;
import store.kanggyoenggu.authservice.dto.oauth.GoogleUserInfo;
import store.kanggyoenggu.authservice.service.oauth.GoogleOAuthService;
import store.kanggyoenggu.authservice.service.JwtService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 구글 OAuth2 인증 컨트롤러 (Spring MVC)
 */
@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final JwtService jwtService;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${google.authorization-uri}")
    private String googleAuthorizationUri;

    @Value("${frontend.callback-url}")
    private String frontendCallbackUrl;

    public GoogleAuthController(GoogleOAuthService googleOAuthService, JwtService jwtService) {
        this.googleOAuthService = googleOAuthService;
        this.jwtService = jwtService;
    }

    /**
     * 구글 로그인 URL 생성
     * POST /auth/google/login
     * 
     * RESTful API 방식: JSON 응답으로 구글 로그인 URL 반환
     * 프론트엔드에서 받아서 처리
     * 
     * 로그인은 상태를 변경하는 작업이므로 POST 메서드 사용
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> googleLogin() {
        String scope = "openid profile email";
        String googleAuthUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                googleAuthorizationUri,
                googleClientId,
                URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8));

        return ResponseEntity.ok(LoginResponse.success(googleAuthUrl));
    }

    /**
     * 구글 OAuth2 콜백 처리 (동기 방식)
     * GET /auth/google/callback?code=xxx (구글 표준)
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(required = false) String code) {
        // GET 방식 (구글 표준)
        if (code != null) {
            return processCallback(code);
        }

        // code가 없는 경우 에러
        String errorUrl = String.format(
                "%s?error=%s",
                frontendCallbackUrl,
                URLEncoder.encode("missing_code", StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(errorUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 공통 콜백 처리 메서드
     */
    private ResponseEntity<Void> processCallback(String code) {
        try {
            // frontendCallbackUrl 유효성 검사
            if (frontendCallbackUrl == null || frontendCallbackUrl.trim().isEmpty()) {
                System.err.println("ERROR: frontend.callback-url이 설정되지 않았습니다.");
                return createErrorResponse("FRONTEND_CALLBACK_URL_NOT_CONFIGURED");
            }

            // 1. 액세스 토큰 요청 (동기 처리)
            GoogleTokenResponse tokenResponse = googleOAuthService.getAccessToken(code);
            String accessToken = tokenResponse.getAccessToken();

            // 구글 액세스 토큰 출력
            System.out.println("========== 구글 액세스 토큰 ==========");
            System.out.println("Access Token: " + accessToken);
            System.out.println("Token Type: " + tokenResponse.getTokenType());
            System.out.println("Expires In: " + tokenResponse.getExpiresIn());
            System.out.println("========================================");

            // 2. 사용자 정보 조회 (동기 처리)
            GoogleUserInfo userInfo = googleOAuthService.getUserInfo(accessToken);

            // 3. 구글 사용자 정보 추출
            String googleId = userInfo.getId();
            String name = userInfo.getName() != null ? userInfo.getName() : "사용자";
            String nickname = name; // 구글은 별명이 없으므로 이름을 별명으로 사용
            String profileImageUrl = userInfo.getPicture() != null ? userInfo.getPicture() : "없음";

            // 구글 사용자 정보 출력 (가입 정보)
            System.out.println("\n========== 구글 가입 정보 ==========");
            System.out.println("구글 ID: " + googleId);
            System.out.println("이름: " + name);
            System.out.println("닉네임: " + nickname);
            System.out.println("프로필 이미지: " + profileImageUrl);
            System.out.println("======================================\n");

            // 4. JWT 토큰 생성 (구글 ID를 Long으로 변환 시도, 실패하면 String 사용)
            Long userId;
            try {
                userId = Long.parseLong(googleId);
            } catch (NumberFormatException e) {
                // 구글 ID가 숫자가 아닌 경우 해시값 사용
                userId = (long) googleId.hashCode();
            }
            String jwtToken = jwtService.generateToken(userId, nickname);

            // 생성된 JWT 토큰 출력
            System.out.println("========== 생성된 JWT 토큰 ==========");
            System.out.println("JWT Token: " + jwtToken);
            System.out.println("Google ID: " + googleId);
            System.out.println("Nickname: " + nickname);
            System.out.println("=====================================\n");

            // 5. 프론트엔드로 리다이렉트 (토큰 포함)
            return createRedirectResponse(frontendCallbackUrl, jwtToken, null);

        } catch (Exception e) {
            // 에러 로깅
            System.err.println("ERROR: 콜백 처리 중 예외 발생: " + e.getMessage());
            e.printStackTrace();

            // 에러 발생 시 프론트엔드로 리다이렉트
            return createRedirectResponse(frontendCallbackUrl, null, "login_failed");
        }
    }

    /**
     * 안전한 리다이렉트 응답 생성
     */
    private ResponseEntity<Void> createRedirectResponse(String baseUrl, String token, String error) {
        try {
            // baseUrl 유효성 검사
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                System.err.println("ERROR: 리다이렉트 URL이 설정되지 않았습니다.");
                baseUrl = "http://localhost:3000/dashboard"; // 기본값
            }

            // URL 생성
            String redirectUrl;
            if (token != null) {
                // 성공: 토큰 포함
                String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
                redirectUrl = String.format("%s?token=%s", baseUrl, encodedToken);
                System.out.println(
                        "리다이렉트 URL (성공): " + redirectUrl.substring(0, Math.min(100, redirectUrl.length())) + "...");
            } else if (error != null) {
                // 실패: 에러 포함
                redirectUrl = String.format("%s?error=%s", baseUrl, URLEncoder.encode(error, StandardCharsets.UTF_8));
                System.out.println("리다이렉트 URL (실패): " + redirectUrl);
            } else {
                // 기본 리다이렉트
                redirectUrl = baseUrl;
                System.out.println("리다이렉트 URL (기본): " + redirectUrl);
            }

            // URI 생성 및 검증
            URI redirectUri;
            try {
                redirectUri = URI.create(redirectUrl);
            } catch (IllegalArgumentException e) {
                System.err.println("ERROR: 잘못된 리다이렉트 URL 형식: " + redirectUrl);
                System.err.println("ERROR: " + e.getMessage());
                // 기본 URL로 폴백
                redirectUri = URI.create("http://localhost:3000/dashboard?error=redirect_url_invalid");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirectUri);

            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            // 리다이렉트 생성 실패 시 기본 에러 응답
            System.err.println("ERROR: 리다이렉트 응답 생성 실패: " + e.getMessage());
            e.printStackTrace();

            HttpHeaders headers = new HttpHeaders();
            try {
                headers.setLocation(URI.create("http://localhost:3000/dashboard?error=redirect_failed"));
            } catch (Exception ex) {
                System.err.println("ERROR: 기본 리다이렉트 URL도 실패: " + ex.getMessage());
            }

            return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<Void> createErrorResponse(String errorMessage) {
        return createRedirectResponse(frontendCallbackUrl, null, errorMessage);
    }

    /**
     * 사용자 정보 조회
     * GET /auth/google/user
     * 
     * Gateway에서 이미 JWT 검증을 완료하고 헤더에 사용자 정보를 추가했으므로,
     * 중복 검증 없이 헤더에서 직접 사용자 정보를 추출합니다.
     */
    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Nickname") String nickname) {

        // Gateway에서 이미 검증 완료, 헤더에서 사용자 정보 추출만 수행
        UserInfoResponse.UserData userData = new UserInfoResponse.UserData(
                userId,
                null, // 구글은 별도의 구글 ID 헤더가 없을 수 있음
                nickname);

        return ResponseEntity.ok(UserInfoResponse.success(userData));
    }

    /**
     * 로그아웃
     * GET /auth/google/logout
     */
    @GetMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }
}
