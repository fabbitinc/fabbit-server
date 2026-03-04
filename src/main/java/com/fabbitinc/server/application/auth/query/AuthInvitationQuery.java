package com.fabbitinc.server.application.auth.query;

import com.fabbitinc.server.domain.auth.repository.AuthInvitationEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthInvitationQuery {

    private final AuthInvitationEndpointRepository repository;

    @Transactional(readOnly = true)
    public Map<String, Object> execute(String operation, Map<String, Object> payload) {
        return repository.query(operation, payload);
    }
}
