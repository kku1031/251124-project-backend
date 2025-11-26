package store.kanggyoenggu.authservice.controller.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.kanggyoenggu.authservice.dto.oauth.KakaoTokenResponse;
import store.kanggyoenggu.authservice.dto.oauth.KakaoUserInfo;
import store.kanggyoenggu.authservice.service.oauth.KakaoOAuthService;
import store.kanggyoenggu.authservice.service.JwtService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 콜백 컨트롤러
 * 카카오 개발자 콘솔에서 /oauth2/kakao/callback으로 설정된 경우를 위한 별도 컨트롤러
 */
@RestController
@RequestMapping("/oauth2")
public class OAuth2CallbackController {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtService jwtService;

    @Value("${frontend.callback-url}")
    private String frontendCallbackUrl;

    public OAuth2CallbackController(KakaoOAuthService kakaoOAuthService, JwtService jwtService) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.jwtService = jwtService;
    }

    /**
     * 카카오 OAuth2 콜백 처리
     * GET /oauth2/kakao/callback?code=xxx (카카오 표준)
     * POST /oauth2/kakao/callback (body에 code 포함, 비표준)
     * 
     * 카카오 개발자 콘솔에서 redirect_uri를 /oauth2/kakao/callback으로 설정한 경우
     */
    @GetMapping("/kakao/callback")
    @PostMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(@RequestParam(required = false) String code,
            @RequestBody(required = false) Map<String, String> body) {
        // GET 방식 (카카오 표준)
        String authCode = code;
        if (authCode == null && body != null && body.containsKey("code")) {
            // POST 방식 (body에서 code 추출)
            authCode = body.get("code");
        }

        if (authCode == null) {
            // 둘 다 없는 경우 에러
            String errorUrl = String.format(
                    "%s?error=%s",
                    frontendCallbackUrl,
                    URLEncoder.encode("missing_code", StandardCharsets.UTF_8));
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        return processCallback(authCode);
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
            KakaoTokenResponse tokenResponse = kakaoOAuthService.getAccessToken(code);
            String accessToken = tokenResponse.getAccessToken();

            // 카카오 액세스 토큰 출력
            System.out.println("========== 카카오 액세스 토큰 ==========");
            System.out.println("Access Token: " + accessToken);
            System.out.println("Token Type: " + tokenResponse.getTokenType());
            System.out.println("Expires In: " + tokenResponse.getExpiresIn());
            System.out.println("========================================");

            // 2. 사용자 정보 조회 (동기 처리)
            KakaoUserInfo userInfo = kakaoOAuthService.getUserInfo(accessToken);

            // 3. 카카오 사용자 정보 추출
            Long kakaoId = userInfo.getId();
            KakaoUserInfo.KakaoAccount kakaoAccount = userInfo.getKakaoAccount();
            KakaoUserInfo.Profile profile = kakaoAccount != null ? kakaoAccount.getProfile() : null;

            String nickname = profile != null ? profile.getNickname() : "사용자";
            String profileImageUrl = profile != null
                    ? (profile.getProfileImageUrl() != null ? profile.getProfileImageUrl() : "없음")
                    : "없음";
            String thumbnailImageUrl = profile != null
                    ? (profile.getThumbnailImageUrl() != null ? profile.getThumbnailImageUrl() : "없음")
                    : "없음";

            // 카카오 사용자 정보 출력 (가입 정보)
            System.out.println("\n========== 카카오 가입 정보 ==========");
            System.out.println("카카오 ID: " + kakaoId);
            System.out.println("닉네임: " + nickname);
            System.out.println("프로필 이미지: " + profileImageUrl);
            System.out.println("썸네일 이미지: " + thumbnailImageUrl);
            System.out.println("연결 시간: " + (userInfo.getConnectedAt() != null ? userInfo.getConnectedAt() : "없음"));
            System.out.println("======================================\n");

            // 4. JWT 토큰 생성
            String jwtToken = jwtService.generateToken(kakaoId, nickname);

            // 생성된 JWT 토큰 출력
            System.out.println("========== 생성된 JWT 토큰 ==========");
            System.out.println("JWT Token: " + jwtToken);
            System.out.println("Kakao ID: " + kakaoId);
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
}
