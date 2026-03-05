package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthVerificationService;
import com.fabbitinc.server.application.auth.usecase.command.SendVerificationCommand;
import com.fabbitinc.server.application.auth.usecase.result.SendVerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class SendVerificationUseCase {

    private final AuthVerificationService authVerificationService;

    public SendVerificationResult execute(SendVerificationCommand command) {
        authVerificationService.sendVerification(command.email(), command.turnstileToken());
        return new SendVerificationResult("인증코드가 발송되었습니다");
    }
}
