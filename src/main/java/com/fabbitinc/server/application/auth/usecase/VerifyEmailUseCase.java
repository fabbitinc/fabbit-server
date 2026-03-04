package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.VerifyEmailRequest;
import com.fabbitinc.server.application.auth.dto.response.VerifyEmailResponse;
import com.fabbitinc.server.application.auth.service.AuthVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private final AuthVerificationService authVerificationService;

    @Transactional
    public VerifyEmailResponse execute(VerifyEmailRequest request) {
        return authVerificationService.verifyEmail(request);
    }
}
