package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAsyncExecutionService {

    private final ChatAgentService chatAgentService;

    @Async("chatTaskExecutor")
    public void runAsync(UUID runId, String schemaName) {
        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            chatAgentService.processRun(runId);
        } catch (Exception ex) {
            log.error("챗 비동기 실행 실패: runId={}, schema={}", runId, schemaName, ex);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
