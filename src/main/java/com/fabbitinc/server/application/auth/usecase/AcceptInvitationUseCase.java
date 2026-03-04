package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthInvitationEndpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AcceptInvitationUseCase {

    private final AuthInvitationEndpointService service;

    @Transactional
    public Map<String, Object> execute(String operation, Map<String, Object> payload) {
        return service.execute(operation, payload);
    }
}
