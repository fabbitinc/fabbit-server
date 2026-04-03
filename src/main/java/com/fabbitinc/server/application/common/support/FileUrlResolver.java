package com.fabbitinc.server.application.common.support;

import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.file.port.StoragePort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUrlResolver {

    private final AppProperties appProperties;
    private final StoragePort storagePort;

    public String resolve(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }

        Duration expireDuration = Duration.ofMinutes(Math.max(1, appProperties.storageReadUrlExpireMinutes()));
        return storagePort.generateGetPresignedUrl(fileKey, expireDuration);
    }
}
