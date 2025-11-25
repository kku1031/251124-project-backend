package store.kanggyoenggu.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 카카오 인증 컨트롤러
 * - 실제 카카오 API 호출 및 처리
 * - JWT 토큰 생성 및 발급
 */
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoController {

    private final KakaoService kakaoService;
    private final JwtService jwtService;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.authorization-uri}")
    private String kakaoAuthorizationUri;

    @Value("${frontend.callback-url}")
    private String frontendCallbackUrl;

    public KakaoController(KakaoService kakaoService, JwtService jwtService) {
        this.kakaoService = kakaoService;
        this.jwtService = jwtService;
    }

    /**
     * 카카오 로그인 URL 생성 및 리다이렉트
     * GET /api/auth/kakao/login
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> kakaoLogin() {
        String kakaoAuthUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code",
                kakaoAuthorizationUri,
                kakaoRestApiKey,
                URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "카카오 로그인 URL 생성 성공");
        response.put("authUrl", kakaoAuthUrl);

        return ResponseEntity.ok(response);
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
                .<ResponseEntity<Void>>map(userInfo -> {
                    // 카카오 사용자 정보 추출
                    Long kakaoId = ((Number) userInfo.get("id")).longValue();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) (kakaoAccount != null
                            ? kakaoAccount.get("profile")
                            : null);

                    String nickname = profile != null ? (String) profile.get("nickname") : "사용자";

                    // JWT 토큰 생성
                    String jwtToken = jwtService.generateToken(kakaoId, nickname);

                    // 프론트엔드로 리다이렉트 (토큰 포함)
                    String redirectUrl = String.format(
                            "%s?token=%s",
                            frontendCallbackUrl,
                            URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));

                    return ResponseEntity.<Void>status(HttpStatus.FOUND)
                            .header("Location", redirectUrl)
                            .build();
                })
                .onErrorResume(e -> {
                    // 에러 발생 시 프론트엔드로 리다이렉트
                    String errorUrl = String.format(
                            "%s?error=%s",
                            frontendCallbackUrl,
                            URLEncoder.encode("login_failed", StandardCharsets.UTF_8));

                    return Mono.just(ResponseEntity.<Void>status(HttpStatus.FOUND)
                            .header("Location", errorUrl)
                            .build());
                });
    }

    /**
     * 사용자 정보 조회 (JWT 토큰 기반)
     * GET /api/auth/kakao/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Map<String, Object> response = new HashMap<>();

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.put("success", false);
            response.put("message", "토큰이 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = authorization.substring(7);

        try {
            Map<String, Object> claims = jwtService.parseToken(token);

            Map<String, Object> user = new HashMap<>();
            user.put("id", claims.get("sub"));
            user.put("kakaoId", claims.get("kakaoId"));
            user.put("nickname", claims.get("nickname"));

            response.put("success", true);
            response.put("message", "사용자 정보 조회 성공");
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "토큰이 유효하지 않습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * 로그아웃
     * POST /api/auth/kakao/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃 성공");

        return ResponseEntity.ok(response);
    }
}
