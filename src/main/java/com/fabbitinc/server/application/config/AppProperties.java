package com.fabbitinc.server.application.config;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("lvh.me") String baseDomain,
        @DefaultValue("10") int emailVerificationExpireMinutes,
        @DefaultValue("5") int emailVerificationMaxAttempts,
        @DefaultValue("60") int emailVerificationCooldownSeconds
) {
}
