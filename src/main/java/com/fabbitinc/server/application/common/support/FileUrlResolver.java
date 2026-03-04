package com.fabbitinc.server.application.common.support;

import com.fabbitinc.server.application.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUrlResolver {

    private final AppProperties appProperties;

    public String resolve(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }

        String publicUrl = appProperties.storagePublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            return trimTrailingSlash(publicUrl) + "/" + fileKey;
        }

        return trimTrailingSlash(appProperties.storageEndpoint())
                + "/"
                + appProperties.storageBucket()
                + "/"
                + fileKey;
    }

    private String trimTrailingSlash(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    }
}
