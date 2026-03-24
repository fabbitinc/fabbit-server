package com.fabbitinc.server.application.config;

import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @DefaultValue("change-me-in-production") String secretKey,
        @DefaultValue("fabbit") String issuer,
        @DefaultValue("15") int accessTokenExpireMinutes,
        @DefaultValue("7") int refreshTokenExpireDays
) {

    private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Set<String> WEAK_SECRETS = Set.of(
            "change-me-in-production",
            "super-secret-change-me-in-production",
            "secret",
            "changeme"
    );

    public JwtProperties {
        if (WEAK_SECRETS.contains(secretKey) || estimateEntropy(secretKey) < MINIMUM_SECRET_BYTES) {
            log.warn(
                    "[SECURITY] JWT 시크릿 키가 안전하지 않습니다. "
                            + "프로덕션 환경에서는 최소 32바이트 이상의 랜덤 값을 사용하세요. "
                            + "(생성 예시: openssl rand -base64 32)"
            );
        }
    }

    private static int estimateEntropy(String key) {
        try {
            return Base64.getDecoder().decode(key).length;
        } catch (IllegalArgumentException e) {
            return key.getBytes().length;
        }
    }
}
