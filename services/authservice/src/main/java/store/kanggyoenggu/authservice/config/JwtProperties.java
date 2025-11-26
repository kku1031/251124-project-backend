package store.kanggyoenggu.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 설정 Properties
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    
    /**
     * JWT Secret Key
     */
    private String secret;
    
    /**
     * JWT 만료 시간 (밀리초, 기본값: 24시간)
     */
    private Long expiration = 86400000L;
}

