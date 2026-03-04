package com.fabbitinc.server.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @DefaultValue("change-me-in-production") String secretKey,
        @DefaultValue("fabbit") String issuer,
        @DefaultValue("15") int accessTokenExpireMinutes,
        @DefaultValue("7") int refreshTokenExpireDays
) {
}
