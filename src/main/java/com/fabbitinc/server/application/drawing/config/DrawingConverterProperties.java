package com.fabbitinc.server.application.drawing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.drawing-converter")
public record DrawingConverterProperties(
        @DefaultValue("/opt/qcad") String qcadPath,
        String ezdxfBinPath,
        @DefaultValue("2") int maxConcurrent,
        @DefaultValue("/tmp/drawing-converter") String tempDir,
        String threeDConverterBinPath,
        @DefaultValue("300") long commandTimeoutSeconds,
        @DefaultValue("420") long pipelineTimeoutSeconds,
        @DefaultValue("24") long tempDirCleanupMaxAgeHours
) {
    @ConstructorBinding
    public DrawingConverterProperties {
    }

    public DrawingConverterProperties(
            String qcadPath,
            int maxConcurrent,
            String tempDir,
            String threeDConverterBinPath,
            long commandTimeoutSeconds,
            long pipelineTimeoutSeconds,
            long tempDirCleanupMaxAgeHours
    ) {
        this(
                qcadPath,
                null,
                maxConcurrent,
                tempDir,
                threeDConverterBinPath,
                commandTimeoutSeconds,
                pipelineTimeoutSeconds,
                tempDirCleanupMaxAgeHours
        );
    }
}
