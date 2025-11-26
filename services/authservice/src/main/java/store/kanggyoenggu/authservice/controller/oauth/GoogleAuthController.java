package store.kanggyoenggu.authservice.controller.oauth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 구글 OAuth2 인증 컨트롤러 (Placeholder)
 */
@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {

    /**
     * 구글 로그인 (미구현)
     * GET /auth/google/login
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> googleLogin() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "구글 로그인은 아직 구현되지 않았습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 구글 OAuth2 콜백 (미구현)
     * GET /auth/google/callback
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> googleCallback(@RequestParam(required = false) String code) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "구글 로그인 콜백은 아직 구현되지 않았습니다.");

        return ResponseEntity.ok(response);
    }
}

