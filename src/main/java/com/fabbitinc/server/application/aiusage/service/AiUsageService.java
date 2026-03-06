package com.fabbitinc.server.application.aiusage.service;

import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.domain.aiusage.model.AiUsageLog;
import com.fabbitinc.server.domain.aiusage.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageLogRepository aiUsageLogRepository;

    public void record(RecordAiUsageInput input) {
        aiUsageLogRepository.save(AiUsageLog.create(
                input.orgId(),
                input.userId(),
                input.category().name(),
                input.feature(),
                input.model(),
                input.inputTokens(),
                input.outputTokens(),
                input.category().creditCostDecimal()
        ));
    }
}
