package com.fabbitinc.server.application.synthesis.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.application.synthesis.service.SynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StartSynthesisUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SynthesisService synthesisService;

    @Transactional
    public SynthesisBatchStartResponse execute(SynthesisStartRequest request) {
        currentAuthProvider.getCurrentAuth();
        return synthesisService.startSynthesis(request);
    }
}
