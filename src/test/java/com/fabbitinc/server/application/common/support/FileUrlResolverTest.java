package com.fabbitinc.server.application.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.file.port.StoragePort;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileUrlResolverTest {

    @Mock
    private StoragePort storagePort;

    private FileUrlResolver fileUrlResolver;

    @BeforeEach
    void setUp() {
        fileUrlResolver = new FileUrlResolver(appProperties(15), storagePort);
    }

    @Test
    void resolve_파일키가_비어있으면_null을_반환한다() {
        assertNull(fileUrlResolver.resolve(null));
        assertNull(fileUrlResolver.resolve(""));
        assertNull(fileUrlResolver.resolve("   "));
        verifyNoInteractions(storagePort);
    }

    @Test
    void resolve_스토리지_presigned_get_url을_반환한다() {
        String fileKey = "tenants/org/uploaded/file.png";
        when(storagePort.generateGetPresignedUrl(fileKey, Duration.ofMinutes(15)))
                .thenReturn("https://signed.example/file.png");

        String result = fileUrlResolver.resolve(fileKey);

        assertEquals("https://signed.example/file.png", result);
    }

    private AppProperties appProperties(int storageReadUrlExpireMinutes) {
        return new AppProperties(
                "lvh.me",
                10,
                5,
                10,
                7,
                "http://localhost:5173",
                "localhost",
                1025,
                "",
                "",
                false,
                "noreply@fabbit.io",
                "Fabbit",
                false,
                "",
                "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "fabbit",
                "",
                "",
                true,
                "https://openrouter.ai/api/v1",
                "openai/gpt-5.4-nano",
                30,
                "openai/gpt-5.4-nano",
                storageReadUrlExpireMinutes
        );
    }
}
