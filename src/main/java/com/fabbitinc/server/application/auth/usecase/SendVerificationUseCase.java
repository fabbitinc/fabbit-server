package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.SendVerificationRequest;
import com.fabbitinc.server.application.auth.dto.response.SendVerificationResponse;
import com.fabbitinc.server.application.auth.service.AuthVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SendVerificationUseCase {

    private final AuthVerificationService authVerificationService;

    @Transactional
    public SendVerificationResponse execute(SendVerificationRequest request) {
        return authVerificationService.sendVerification(request);
    }
}
