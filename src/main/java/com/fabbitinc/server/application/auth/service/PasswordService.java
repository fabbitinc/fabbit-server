package com.fabbitinc.server.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder;

    public String hash(String plain) {
        return passwordEncoder.encode(plain);
    }

    public boolean matches(String plain, String hashed) {
        return passwordEncoder.matches(plain, hashed);
    }
}
