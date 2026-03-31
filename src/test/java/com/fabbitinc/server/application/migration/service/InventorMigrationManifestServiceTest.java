package com.fabbitinc.server.application.migration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventorMigrationManifestServiceTest {

    private final InventorMigrationManifestService service = new InventorMigrationManifestService();

    @Test
    void analyze_same_directory_same_basename_drawing을_매칭한다() {
        UUID modelFileId = UUID.randomUUID();
        UUID drawingFileId = UUID.randomUUID();
        InventorMigrationSession session = new InventorMigrationSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Motor Assembly",
                "Motor Assembly.ipj",
                "2024",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                List.of(
                        new InventorManifestFile("Parts/Shaft.ipt", "Shaft.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha()),
                        new InventorManifestFile("Parts/Shaft.dwg", "Shaft.dwg", InventorManifestFileType.DRAWING, "application/octet-stream", 20L, sha())
                ),
                Map.of("Parts/Shaft.ipt", modelFileId, "Parts/Shaft.dwg", drawingFileId)
        );

        var analysis = service.analyze(session);

        assertEquals(1, analysis.items().size());
        assertEquals(List.of("Parts/Shaft.dwg"), analysis.items().get(0).matchedDrawingPaths());
        assertEquals(List.of(drawingFileId), analysis.items().get(0).matchedDrawingFileIds());
        assertEquals(0, analysis.orphanDrawings().size());
    }

    @Test
    void analyze_다른_디렉터리의_drawing은_orphan으로_분류한다() {
        UUID modelFileId = UUID.randomUUID();
        UUID drawingFileId = UUID.randomUUID();
        InventorMigrationSession session = new InventorMigrationSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Motor Assembly",
                "Motor Assembly.ipj",
                "2024",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                List.of(
                        new InventorManifestFile("Parts/Shaft.ipt", "Shaft.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha()),
                        new InventorManifestFile("Drawings/Shaft.dwg", "Shaft.dwg", InventorManifestFileType.DRAWING, "application/octet-stream", 20L, sha())
                ),
                Map.of("Parts/Shaft.ipt", modelFileId, "Drawings/Shaft.dwg", drawingFileId)
        );

        var analysis = service.analyze(session);

        assertEquals(1, analysis.items().size());
        assertEquals(0, analysis.items().get(0).matchedDrawingPaths().size());
        assertEquals(1, analysis.orphanDrawings().size());
        assertEquals("Drawings/Shaft.dwg", analysis.orphanDrawings().get(0).path());
    }

    @Test
    void validateStartInput_importable_파일이_없으면_예외를_던진다() {
        AppException ex = assertThrows(AppException.class, () -> service.validateStartInput(
                "Motor Assembly",
                "Motor Assembly.ipj",
                List.of(new InventorManifestFile("Docs/spec.pdf", "spec.pdf", InventorManifestFileType.ATTACHMENT, "application/pdf", 10L, sha()))
        ));

        assertEquals("가져오기 대상 PART 또는 ASSEMBLY 파일이 1개 이상 필요합니다", ex.getMessage());
    }

    @Test
    void derivePartNumber_확장자만_제거한다() {
        String partNumber = service.derivePartNumber(
                new InventorManifestFile("Parts/shaft.v2.ipt", "shaft.v2.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha())
        );

        assertEquals("shaft.v2", partNumber);
    }

    private String sha() {
        return "6d2bc3f13b59bf38368ffce5aa7498479f880c6da14961fb1bc696ff44e43173";
    }
}
