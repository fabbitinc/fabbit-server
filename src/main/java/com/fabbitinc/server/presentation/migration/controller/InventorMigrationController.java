package com.fabbitinc.server.presentation.migration.controller;

import com.fabbitinc.server.application.migration.usecase.CommitInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.PreviewInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.StartInventorMigrationUseCase;
import com.fabbitinc.server.application.migration.usecase.command.CommitInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.command.PreviewInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.command.StartInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.result.CommitInventorMigrationResult;
import com.fabbitinc.server.application.migration.usecase.result.PreviewInventorMigrationResult;
import com.fabbitinc.server.application.migration.usecase.result.StartInventorMigrationResult;
import com.fabbitinc.server.presentation.migration.request.CommitInventorMigrationRequest;
import com.fabbitinc.server.presentation.migration.request.StartInventorMigrationRequest;
import com.fabbitinc.server.presentation.migration.response.InventorMigrationCommitResponse;
import com.fabbitinc.server.presentation.migration.response.InventorMigrationPreviewResponse;
import com.fabbitinc.server.presentation.migration.response.InventorMigrationStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/migrations/inventor")
@Tag(name = "inventor-migrations", description = "Inventor 마이그레이션 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class InventorMigrationController {

    private final StartInventorMigrationUseCase startInventorMigrationUseCase;
    private final PreviewInventorMigrationUseCase previewInventorMigrationUseCase;
    private final CommitInventorMigrationUseCase commitInventorMigrationUseCase;

    @Operation(
            operationId = "inventorMigrationCreate",
            summary = "Inventor 마이그레이션 세션을 생성합니다",
            description = "매니페스트를 수신하고 업로드용 presigned URL을 일괄 발급합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventorMigrationStartResponse create(
            @Valid @RequestBody StartInventorMigrationRequest request
    ) {
        StartInventorMigrationResult result = startInventorMigrationUseCase.execute(new StartInventorMigrationCommand(
                request.projectName(),
                request.ipjPath(),
                request.inventorVersion(),
                request.files().stream()
                        .map(item -> item.toManifestFile())
                        .toList()
        ));
        return new InventorMigrationStartResponse(
                result.sessionId(),
                result.projectName(),
                result.totalFileCount(),
                result.importableFileCount(),
                result.uploadTargets().stream()
                        .map(item -> new InventorMigrationStartResponse.UploadTargetResponse(
                                item.path(),
                                item.fileId(),
                                item.uploadUrl(),
                                item.fileKey()
                        ))
                        .toList()
        );
    }

    @Operation(
            operationId = "inventorMigrationGetPreview",
            summary = "Inventor 마이그레이션 미리보기를 조회합니다",
            description = "세션 기준으로 import 대상, 중복, orphan drawing, 업로드 상태를 조회합니다"
    )
    @GetMapping("/preview")
    public InventorMigrationPreviewResponse get(
            @Parameter(description = "마이그레이션 세션 ID")
            @RequestParam UUID sessionId
    ) {
        PreviewInventorMigrationResult result = previewInventorMigrationUseCase.execute(
                new PreviewInventorMigrationCommand(sessionId)
        );
        return new InventorMigrationPreviewResponse(
                result.sessionId(),
                result.projectName(),
                new InventorMigrationPreviewResponse.SummaryResponse(
                        result.summary().totalFileCount(),
                        result.summary().importableFileCount(),
                        result.summary().readyItemCount(),
                        result.summary().warningCount(),
                        result.summary().errorCount()
                ),
                result.items().stream()
                        .map(item -> new InventorMigrationPreviewResponse.ItemResponse(
                                item.path(),
                                item.fileType(),
                                item.derivedPartNumber(),
                                item.modelFileId(),
                                item.uploaded(),
                                item.status(),
                                item.message(),
                                item.drawingFileIds(),
                                item.drawingPaths()
                        ))
                        .toList(),
                result.orphanDrawings().stream()
                        .map(item -> new InventorMigrationPreviewResponse.OrphanDrawingResponse(
                                item.path(),
                                item.fileId(),
                                item.uploaded(),
                                item.message()
                        ))
                        .toList(),
                result.readyToCommit()
        );
    }

    @Operation(
            operationId = "inventorMigrationCommit",
            summary = "Inventor 마이그레이션을 커밋합니다",
            description = "프로젝트와 Part를 생성하고 업로드된 CAD/도면 파일을 연결합니다"
    )
    @PostMapping("/commit")
    @ResponseStatus(HttpStatus.CREATED)
    public InventorMigrationCommitResponse commit(
            @Valid @RequestBody CommitInventorMigrationRequest request
    ) {
        CommitInventorMigrationResult result = commitInventorMigrationUseCase.execute(
                new CommitInventorMigrationCommand(request.sessionId())
        );
        return new InventorMigrationCommitResponse(
                result.projectId(),
                result.createdPartIds(),
                new InventorMigrationCommitResponse.SummaryResponse(
                        result.summary().createdPartCount(),
                        result.summary().orphanDrawingCount()
                )
        );
    }
}
