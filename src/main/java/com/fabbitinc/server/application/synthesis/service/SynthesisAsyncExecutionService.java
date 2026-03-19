package com.fabbitinc.server.application.synthesis.service;

import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynthesisAsyncExecutionService {

    private final SynthesisExecutionService synthesisV2ExecutionService;

    @Async("synthesisTaskExecutor")
    public void runJobAsync(
            UUID jobId,
            String schemaName,
            Map<String, String> rootContext,
            boolean overwrite
    ) {
        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            synthesisV2ExecutionService.runJob(jobId, rootContext, overwrite);
        } catch (Exception ex) {
            log.error("합성 비동기 실행 실패: jobId={}, schema={}", jobId, schemaName, ex);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
