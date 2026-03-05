package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthVerificationService;
import com.fabbitinc.server.application.auth.usecase.command.VerifyEmailCommand;
import com.fabbitinc.server.application.auth.usecase.result.VerifyEmailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class VerifyEmailUseCase {

    private final AuthVerificationService authVerificationService;

    public VerifyEmailResult execute(VerifyEmailCommand command) {
        AuthVerificationService.VerifyEmailOutput output =
                authVerificationService.verifyEmail(command.email(), command.code());
        return new VerifyEmailResult(output.verificationToken(), output.email());
    }
}
