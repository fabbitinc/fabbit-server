package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.LoginRequest;
import com.fabbitinc.server.application.auth.service.AuthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthAccountService authAccountService;

    @Transactional
    public Object execute(LoginRequest request, String slug) {
        return authAccountService.login(request, slug);
    }
}
