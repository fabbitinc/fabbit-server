package com.fabbitinc.server.application.activation.usecase;

import com.fabbitinc.server.application.activation.dto.response.HealthCheckResponse;
import com.fabbitinc.server.application.activation.service.ActivationService;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HealthCheckUseCase {

    private final AuthTokenParser authTokenParser;
    private final ActivationService activationService;

    @Transactional(readOnly = true)
    public HealthCheckResponse execute(String authorizationHeader) {
        authTokenParser.requireAuth(authorizationHeader);
        return activationService.healthCheck();
    }
}
