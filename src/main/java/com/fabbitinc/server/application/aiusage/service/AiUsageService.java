package com.fabbitinc.server.application.aiusage.service;

import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.domain.aiusage.model.AiUsageEvent;
import com.fabbitinc.server.domain.aiusage.repository.AiUsageEventRepository;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private static final BigDecimal METERED_AI_CREDIT_UNIT_PRICE = BigDecimal.valueOf(5L);

    private final AiUsageEventRepository aiUsageEventRepository;
    private final SubscriptionApi subscriptionApi;

    public void record(RecordAiUsageInput input) {
        WorkspacePlanType planType = subscriptionApi.getCurrentPlanType(input.orgId());
        BigDecimal creditsUsed = input.category().creditCostDecimal();
        BigDecimal billableAmount = planType.aiBillingMode().isMetered()
                ? creditsUsed.multiply(METERED_AI_CREDIT_UNIT_PRICE)
                : BigDecimal.ZERO;

        aiUsageEventRepository.save(AiUsageEvent.create(
                input.orgId(),
                input.userId(),
                planType,
                subscriptionApi.getCurrentSeatType(input.orgId(), input.userId()),
                input.category().name(),
                input.feature(),
                input.model(),
                input.inputTokens(),
                input.outputTokens(),
                creditsUsed,
                billableAmount
        ));
    }
}
