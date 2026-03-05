package com.fabbitinc.server.application.config;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("lvh.me") String baseDomain,
        @DefaultValue("10") int emailVerificationExpireMinutes,
        @DefaultValue("5") int emailVerificationMaxAttempts,
        @DefaultValue("60") int emailVerificationCooldownSeconds,
        @DefaultValue("7") int invitationExpireDays,
        @DefaultValue("http://localhost:5173") String invitationBaseUrl,
        @DefaultValue("localhost") String smtpHost,
        @DefaultValue("1025") int smtpPort,
        @DefaultValue("") String smtpUsername,
        @DefaultValue("") String smtpPassword,
        @DefaultValue("false") boolean smtpUseTls,
        @DefaultValue("noreply@fabbit.io") String smtpFromEmail,
        @DefaultValue("Fabbit") String smtpFromName,
        @DefaultValue("false") boolean turnstileEnabled,
        @DefaultValue("") String turnstileSecretKey,
        @DefaultValue("https://challenges.cloudflare.com/turnstile/v0/siteverify") String turnstileVerifyUrl,
        @DefaultValue("http://localhost:9000") String storageEndpoint,
        @DefaultValue("minioadmin") String storageAccessKey,
        @DefaultValue("minioadmin") String storageSecretKey,
        @DefaultValue("fabbit") String storageBucket,
        @DefaultValue("") String storagePublicUrl
) {
}
