package com.fabbitinc.server.application.drawing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.drawing-converter")
public record DrawingConverterProperties(
        @DefaultValue("/opt/qcad") String qcadPath,
        @DefaultValue("2") int maxConcurrent,
        @DefaultValue("/tmp/drawing-converter") String tempDir,
        String threeDConverterBinPath,
        @DefaultValue("420") long pipelineTimeoutSeconds
) {
}
