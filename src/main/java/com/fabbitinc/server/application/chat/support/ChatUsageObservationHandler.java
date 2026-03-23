package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.aiusage.api.AiUsageApi;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.stereotype.Component;

/**
 * 모든 LLM 호출 완료 시 자동으로 usage 기록 및 크레딧 차감을 수행합니다.
 * {@link ChatUsageContextHolder}에 비즈니스 컨텍스트가 설정되어 있어야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUsageObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final AiUsageApi aiUsageApi;
    private final OrganizationApi organizationApi;

    @Override
    public void onStop(ChatModelObservationContext context) {
        ChatUsageContextHolder.UsageContext usageContext = ChatUsageContextHolder.get();
        if (usageContext == null) {
            return;
        }

        if (context.getResponse() == null || context.getResponse().getMetadata() == null) {
            return;
        }

        Usage usage = context.getResponse().getMetadata().getUsage();
        if (usage == null) {
            return;
        }

        String model = context.getResponse().getMetadata().getModel();
        int inputTokens = usage.getPromptTokens() == null ? 0 : usage.getPromptTokens();
        int outputTokens = usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens();

        try {
            organizationApi.consumeCredits(usageContext.orgId(), usageContext.category());
            aiUsageApi.record(new RecordAiUsageInput(
                    usageContext.orgId(),
                    usageContext.userId(),
                    usageContext.category(),
                    usageContext.feature(),
                    model == null ? "" : model,
                    inputTokens,
                    outputTokens
            ));
        } catch (RuntimeException ex) {
            log.warn("event=chat_usage_record_failed model={} input_tokens={} output_tokens={} reason={}",
                    model, inputTokens, outputTokens, ex.getMessage());
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }
}
