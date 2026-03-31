package com.fabbitinc.server.application.migration.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import com.fabbitinc.server.application.migration.service.InventorMigrationSessionService;
import com.fabbitinc.server.application.migration.service.InventorMigrationValidationService;
import com.fabbitinc.server.application.migration.usecase.command.PreviewInventorMigrationCommand;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreviewInventorMigrationUseCaseTest {

    @Test
    void execute_ready_preview를_반환한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        FileRepository fileRepository = mock(FileRepository.class);
        PartRepository partRepository = mock(PartRepository.class);
        InventorMigrationSessionService sessionService = new InventorMigrationSessionService();
        InventorMigrationValidationService validationService = new InventorMigrationValidationService(fileRepository, partRepository);
        PreviewInventorMigrationUseCase useCase = new PreviewInventorMigrationUseCase(currentAuthProvider, sessionService, validationService);

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AuthContext auth = new AuthContext(userId, "test@test.com", orgId, null);
        when(currentAuthProvider.getCurrentAuth()).thenReturn(auth);

        UUID fileId = UUID.randomUUID();
        var activeSession = sessionService.createSession(
                auth,
                "Motor Assembly",
                "Motor Assembly.ipj",
                "2024",
                List.of(new InventorManifestFile("Parts/Shaft.ipt", "Shaft.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha())),
                Map.of("Parts/Shaft.ipt", fileId)
        );

        File file = File.create(fileId, "Shaft.ipt", "tenants/test/raw_data/1/Shaft.ipt", "application/octet-stream", 10L, sha());
        file.markUploaded();
        when(fileRepository.findByIdIn(List.of(fileId))).thenReturn(List.of(file));
        when(partRepository.findByPartNumberIn(List.of("Shaft"))).thenReturn(List.of());

        var result = useCase.execute(new PreviewInventorMigrationCommand(activeSession.sessionId()));

        assertEquals(true, result.readyToCommit());
        assertEquals(1, result.items().size());
        assertEquals("READY", result.items().get(0).status());
    }

    private String sha() {
        return "6d2bc3f13b59bf38368ffce5aa7498479f880c6da14961fb1bc696ff44e43173";
    }
}
