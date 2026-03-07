package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawingAsyncConversionService {

    private final DrawingConversionService drawingConversionService;

    @Async("drawingTaskExecutor")
    public void convertDrawingAsync(UUID drawingId, String schemaName) {
        TenantContextHolder.setCurrentSchema(schemaName);
        try {
            drawingConversionService.convertDrawing(drawingId);
        } catch (Exception ex) {
            log.error("event=drawing_conversion_async_failed drawing_id={} schema={}", drawingId, schemaName, ex);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
