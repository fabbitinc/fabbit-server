package com.fabbitinc.server.application.auth.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    private final BCryptPasswordEncoder passwordEncoder;

    public String hash(String plain) {
        return passwordEncoder.encode(plain);
    }

    public boolean matches(String plain, String hashed) {
        return passwordEncoder.matches(plain, hashed);
    }
}
