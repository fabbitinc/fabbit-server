package com.fabbitinc.server.application.aiusage.api;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiUsageApi {

    private final AiUsageService aiUsageService;

    public void record(RecordAiUsageInput input) {
        aiUsageService.record(input);
    }
}
