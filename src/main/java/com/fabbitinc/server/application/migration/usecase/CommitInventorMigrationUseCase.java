package com.fabbitinc.server.application.migration.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import com.fabbitinc.server.application.migration.service.InventorMigrationSessionService;
import com.fabbitinc.server.application.migration.service.InventorMigrationValidationService;
import com.fabbitinc.server.application.migration.support.InventorMigrationAnalyzer;
import com.fabbitinc.server.application.migration.usecase.command.CommitInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.result.CommitInventorMigrationResult;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CommitInventorMigrationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final InventorMigrationSessionService inventorMigrationSessionService;
    private final InventorMigrationValidationService inventorMigrationValidationService;
    private final InventorMigrationAnalyzer inventorMigrationAnalyzer = new InventorMigrationAnalyzer();
    private final ProjectService projectService;
    private final PartService partService;

    public CommitInventorMigrationResult execute(CommitInventorMigrationCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        InventorMigrationSession session = inventorMigrationSessionService.getAccessibleSession(command.sessionId(), auth);
        InventorMigrationAnalyzer.Analysis analysis = inventorMigrationAnalyzer.analyze(session);
        inventorMigrationValidationService.validateCommitReady(session, analysis);

        String description = buildProjectDescription(session);
        var project = projectService.createProject(auth.userId(), session.projectName(), description);

        List<UUID> createdPartIds = new ArrayList<>();
        for (InventorMigrationAnalyzer.ImportItem item : analysis.items()) {
            PartRevision revision = partService.createPart(
                    new CreatePartInput(
                            item.derivedPartNumber(),
                            null,
                            PartItemType.MANUFACTURED,
                            item.derivedPartNumber(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Inventor migration import"
                    ),
                    auth.userId()
            );
            List<UUID> attachmentFileIds = new ArrayList<>();
            attachmentFileIds.add(item.modelFileId());
            attachmentFileIds.addAll(item.matchedDrawingFileIds());
            partService.attachFiles(revision.getPartId(), revision.getId(), attachmentFileIds);
            createdPartIds.add(revision.getPartId());
        }

        projectService.linkParts(project.getId(), createdPartIds);
        inventorMigrationSessionService.removeSession(session.sessionId());

        return new CommitInventorMigrationResult(
                project.getId(),
                createdPartIds,
                new CommitInventorMigrationResult.Summary(createdPartIds.size(), analysis.orphanDrawings().size())
        );
    }

    private String buildProjectDescription(InventorMigrationSession session) {
        return "Imported from %s, %s".formatted(
                session.ipjFileName(),
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }
}
