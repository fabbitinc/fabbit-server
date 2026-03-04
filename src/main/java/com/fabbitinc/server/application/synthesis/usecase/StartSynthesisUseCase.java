package com.fabbitinc.server.application.synthesis.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.application.synthesis.service.SynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StartSynthesisUseCase {

    private final AuthTokenParser authTokenParser;
    private final SynthesisService synthesisService;

    @Transactional
    public SynthesisBatchStartResponse execute(String authorizationHeader, SynthesisStartRequest request) {
        authTokenParser.requireAuth(authorizationHeader);
        return synthesisService.startSynthesis(request);
    }
}
