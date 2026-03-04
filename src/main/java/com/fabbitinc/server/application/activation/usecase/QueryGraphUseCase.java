package com.fabbitinc.server.application.activation.usecase;

import com.fabbitinc.server.application.activation.dto.response.QueryResponse;
import com.fabbitinc.server.application.activation.service.ActivationService;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QueryGraphUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ActivationService activationService;

    @Transactional(readOnly = true)
    public QueryResponse execute(String question) {
        currentAuthProvider.getCurrentAuth();
        return activationService.queryGraph(question);
    }
}
