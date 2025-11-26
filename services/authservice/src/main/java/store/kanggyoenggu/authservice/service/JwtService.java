package store.kanggyoenggu.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증 서비스
 */
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
     * JWT 토큰 파싱 및 검증
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
