package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartPreviewAsyncConversionService {

    private final PartPreviewConversionService partPreviewConversionService;

    @Async("drawingTaskExecutor")
    public void convertPartPreviewAsync(UUID partPreviewId, String schemaName) {
        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            partPreviewConversionService.requestAndConvertPartPreview(partPreviewId);
        } catch (Exception ex) {
            log.error(
                    "event=part_preview_conversion_async_failed part_preview_id={} schema={}",
                    partPreviewId,
                    schemaName,
                    ex
            );
        } finally {
            TenantContextHolder.clear();
        }
    }
}
