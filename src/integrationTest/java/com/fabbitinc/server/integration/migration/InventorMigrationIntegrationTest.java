package com.fabbitinc.server.integration.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.file.usecase.BatchCompleteFilesUseCase;
import com.fabbitinc.server.application.file.usecase.command.BatchCompleteFilesCommand;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import com.fabbitinc.server.application.migration.service.InventorMigrationSessionService;
import com.fabbitinc.server.application.migration.usecase.CommitInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.PreviewInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.StartInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.command.CommitInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.command.PreviewInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.command.StartInventorMigrationCommand;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InventorMigrationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private StartInventorMigrationUseCase startInventorMigrationUseCase;
    @Autowired private BatchCompleteFilesUseCase batchCompleteFilesUseCase;
    @Autowired private CommitInventorMigrationUseCase commitInventorMigrationUseCase;
    @Autowired private PreviewInventorMigrationUseCase previewInventorMigrationUseCase;
    @Autowired private InventorMigrationSessionService inventorMigrationSessionService;
    @Autowired private StoragePort storagePort;
    @Autowired private FileRepository fileRepository;
    @Autowired private PartRepository partRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectPartRepository projectPartRepository;
    @Autowired private PartService partService;

    private User actor;
    private Organization organization;

    @BeforeEach
    void setUp() {
        actor = userRepository.save(User.create("migration-" + UUID.randomUUID() + "@test.com", "hashed", "마이그레이션 사용자"));
        organization = organizationRepository.save(Organization.create(
                "migration-org-" + UUID.randomUUID().toString().substring(0, 8),
                "Migration Org",
                actor.getId(),
                "manufacturing",
                "11-50"
        ));
        testCurrentAuthProvider.set(new AuthContext(actor.getId(), actor.getEmail(), organization.getId(), MembershipRole.OWNER));
        TenantContextHolder.setCurrentSchema(TenantSchemaPolicy.schemaNameForOrgId(organization.getId()));
    }

    @Test
    void start_preview_commit_happy_path() {
        var startResult = startInventorMigrationUseCase.execute(new StartInventorMigrationCommand(
                "Motor Assembly",
                "Motor Assembly.ipj",
                "2024",
                List.of(
                        new InventorManifestFile("Parts/Shaft.ipt", "Shaft.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha()),
                        new InventorManifestFile("Parts/Shaft.dwg", "Shaft.dwg", InventorManifestFileType.DRAWING, "application/octet-stream", 20L, sha())
                )
        ));

        startResult.uploadTargets().forEach(target -> storagePort.putObject(target.fileKey(), new byte[] {1, 2, 3}, "application/octet-stream"));
        batchCompleteFilesUseCase.execute(new BatchCompleteFilesCommand(
                startResult.uploadTargets().stream().map(item -> item.fileId()).toList()
        ));

        var preview = previewInventorMigrationUseCase.execute(new PreviewInventorMigrationCommand(startResult.sessionId()));
        assertTrue(preview.readyToCommit());
        assertEquals(1, preview.items().size());
        assertEquals(1, preview.items().get(0).drawingFileIds().size());

        var commitResult = commitInventorMigrationUseCase.execute(new CommitInventorMigrationCommand(startResult.sessionId()));

        assertEquals(1, commitResult.createdPartIds().size());
        assertEquals(1, projectRepository.findAll().size());
        assertEquals(1, partRepository.findAll().size());
        assertEquals(1, projectPartRepository.findByProjectId(commitResult.projectId()).size());
        assertEquals(2, fileRepository.findByIdIn(startResult.uploadTargets().stream().map(item -> item.fileId()).toList()).stream()
                .filter(file -> file.getOwnerId() != null)
                .count());
        assertThrows(AppException.class, () -> inventorMigrationSessionService.getAccessibleSession(startResult.sessionId(), testCurrentAuthProvider.getCurrentAuth()));
    }

    @Test
    void preview_existing_part_number_conflict를_표시한다() {
        partService.createPart(
                new CreatePartInput("ConflictShaft", null, PartItemType.MANUFACTURED, "ConflictShaft", null, null, null, null, null, null, null),
                actor.getId()
        );

        var startResult = startInventorMigrationUseCase.execute(new StartInventorMigrationCommand(
                "Motor Assembly",
                "Motor Assembly.ipj",
                "2024",
                List.of(new InventorManifestFile("Parts/ConflictShaft.ipt", "ConflictShaft.ipt", InventorManifestFileType.PART, "application/octet-stream", 10L, sha()))
        ));
        startResult.uploadTargets().forEach(target -> storagePort.putObject(target.fileKey(), new byte[] {1, 2, 3}, "application/octet-stream"));
        batchCompleteFilesUseCase.execute(new BatchCompleteFilesCommand(startResult.uploadTargets().stream().map(item -> item.fileId()).toList()));

        var preview = previewInventorMigrationUseCase.execute(new PreviewInventorMigrationCommand(startResult.sessionId()));

        assertEquals(false, preview.readyToCommit());
        assertEquals("ERROR", preview.items().get(0).status());
        assertTrue(preview.items().get(0).message().contains("이미 존재하는 partNumber"));
        assertThrows(AppException.class, () -> commitInventorMigrationUseCase.execute(new CommitInventorMigrationCommand(startResult.sessionId())));
    }

    private String sha() {
        return "6d2bc3f13b59bf38368ffce5aa7498479f880c6da14961fb1bc696ff44e43173";
    }
}
