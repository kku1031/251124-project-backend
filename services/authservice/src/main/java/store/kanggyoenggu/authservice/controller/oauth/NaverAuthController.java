package store.kanggyoenggu.authservice.controller.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.kanggyoenggu.authservice.dto.*;
import store.kanggyoenggu.authservice.dto.oauth.NaverTokenResponse;
import store.kanggyoenggu.authservice.dto.oauth.NaverUserInfo;
import store.kanggyoenggu.authservice.service.oauth.NaverOAuthService;
import store.kanggyoenggu.authservice.service.JwtService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 네이버 OAuth2 인증 컨트롤러 (Spring MVC)
 */
@RestController
@RequestMapping("/auth/naver")
public class NaverAuthController {

    private final NaverOAuthService naverOAuthService;
    private final JwtService jwtService;

    @Value("${naver.client-id}")
    private String naverClientId;

    @Value("${naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${naver.authorization-uri}")
    private String naverAuthorizationUri;

    @Value("${frontend.callback-url}")
    private String frontendCallbackUrl;

    public NaverAuthController(NaverOAuthService naverOAuthService, JwtService jwtService) {
        this.naverOAuthService = naverOAuthService;
        this.jwtService = jwtService;
    }

    /**
     * 네이버 로그인 URL 생성
     * POST /auth/naver/login
     * 
     * RESTful API 방식: JSON 응답으로 네이버 로그인 URL 반환
     * 프론트엔드에서 받아서 처리
     * 
     * 로그인은 상태를 변경하는 작업이므로 POST 메서드 사용
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> naverLogin() {
        // 네이버는 state 파라미터를 권장하지만 필수는 아님
        String state = "STATE"; // 실제로는 랜덤 문자열 사용 권장

        String naverAuthUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
                naverAuthorizationUri,
                naverClientId,
                URLEncoder.encode(naverRedirectUri, StandardCharsets.UTF_8),
                state);

        System.out.println("========== 네이버 로그인 URL 생성 ==========");
        System.out.println("Authorization URI: " + naverAuthorizationUri);
        System.out.println("Client ID: " + naverClientId);
        System.out.println("Redirect URI: " + naverRedirectUri);
        System.out.println("State: " + state);
        System.out.println("Generated URL: " + naverAuthUrl);
        System.out.println("==========================================");

        return ResponseEntity.ok(LoginResponse.success(naverAuthUrl));
    }

    /**
     * 네이버 OAuth2 콜백 처리 (동기 방식)
     * GET /auth/naver/callback?code=xxx&state=xxx (네이버 표준)
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> naverCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        // GET 방식 (네이버 표준)
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
            // 1. 액세스 토큰 요청 (동기 처리)
            NaverTokenResponse tokenResponse = naverOAuthService.getAccessToken(code);
            String accessToken = tokenResponse.getAccessToken();

            // 네이버 액세스 토큰 출력
            System.out.println("========== 네이버 액세스 토큰 ==========");
            System.out.println("Access Token: " + accessToken);
            System.out.println("Token Type: " + tokenResponse.getTokenType());
            System.out.println("Expires In: " + tokenResponse.getExpiresIn());
            System.out.println("========================================");

            // 2. 사용자 정보 조회 (동기 처리)
            NaverUserInfo userInfo = naverOAuthService.getUserInfo(accessToken);

            // 3. 네이버 사용자 정보 추출
            NaverUserInfo.Response response = userInfo.getResponse();
            String naverId = response.getId();
            String nickname = response.getNickname() != null ? response.getNickname() : "사용자";
            String name = response.getName() != null ? response.getName() : nickname;
            String profileImageUrl = response.getProfile_image() != null ? response.getProfile_image() : "없음";

            // 네이버 사용자 정보 출력 (가입 정보)
            System.out.println("\n========== 네이버 가입 정보 ==========");
            System.out.println("네이버 ID: " + naverId);
            System.out.println("이름: " + name);
            System.out.println("닉네임: " + nickname);
            System.out.println("프로필 이미지: " + profileImageUrl);
            System.out.println("======================================\n");

            // 4. JWT 토큰 생성 (네이버 ID를 Long으로 변환 시도, 실패하면 String 사용)
            Long userId;
            try {
                userId = Long.parseLong(naverId);
            } catch (NumberFormatException e) {
                // 네이버 ID가 숫자가 아닌 경우 해시값 사용
                userId = (long) naverId.hashCode();
            }
            String jwtToken = jwtService.generateToken(userId, nickname);

            // 생성된 JWT 토큰 출력
            System.out.println("========== 생성된 JWT 토큰 ==========");
            System.out.println("JWT Token: " + jwtToken);
            System.out.println("Naver ID: " + naverId);
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
     * 사용자 정보 조회
     * GET /auth/naver/user
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
                null, // 네이버는 별도의 네이버 ID 헤더가 없을 수 있음
                nickname);

        return ResponseEntity.ok(UserInfoResponse.success(userData));
    }

    /**
     * 로그아웃
     * GET /auth/naver/logout
     */
    @GetMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }
}
