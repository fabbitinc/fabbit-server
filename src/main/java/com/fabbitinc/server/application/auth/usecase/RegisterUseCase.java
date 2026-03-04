package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.application.auth.dto.response.RegisterResponse;
import com.fabbitinc.server.application.auth.service.AuthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RegisterUseCase {

    private final AuthAccountService authAccountService;

    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        return authAccountService.register(request);
    }
}
