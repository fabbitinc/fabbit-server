package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewProcessingResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewSourcesResponse;

import com.fabbitinc.server.application.part.query.PartPreviewProcessingQuery;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewProcessingCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.usecase.ChangePartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.ClearPartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.UploadPartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.command.ChangePartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.ClearPartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartPreviewFileCommand;
import com.fabbitinc.server.application.part.usecase.command.UploadPartPreviewFileCommand;
import com.fabbitinc.server.presentation.part.request.ChangePartPreviewRequest;
import com.fabbitinc.server.presentation.part.request.UploadPartPreviewFileRequest;
import com.fabbitinc.server.presentation.part.response.PartPreviewProcessingResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewSourcesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revision-previews", description = "공식 부품 리비전 미리보기 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionPreviewController {

    private final PartQuery partQuery;
    private final PartPreviewProcessingQuery partPreviewProcessingQuery;
    private final ChangePartPreviewUseCase changePartPreviewUseCase;
    private final ClearPartPreviewUseCase clearPartPreviewUseCase;
    private final UploadPartPreviewFileUseCase uploadPartPreviewFileUseCase;
    private final DeletePartPreviewFileUseCase deletePartPreviewFileUseCase;

    @Operation(summary = "PATCH /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview", description = "Part 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public PartPreviewResponse update(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Valid @RequestBody ChangePartPreviewRequest request
    ) {
        changePartPreviewUseCase.execute(new ChangePartPreviewCommand(
                partNumber,
                revisionCode,
                null,
                null,
                request.sourceType(),
                request.sourceId()
        ));
        return toPartPreviewResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)).preview());
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/sources", description = "대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/preview/sources")
    public PartPreviewSourcesResponse getSources(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartPreviewSourcesResponse(
                partQuery.getPreviewSources(new PartPreviewSourcesCondition(partNumber, revisionCode, null, null))
        );
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/files", description = "업로드 완료 파일을 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/preview/files")
    public PartPreviewResponse createPreviewFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Valid @RequestBody UploadPartPreviewFileRequest request
    ) {
        uploadPartPreviewFileUseCase.execute(new UploadPartPreviewFileCommand(
                partNumber,
                revisionCode,
                null,
                null,
                request.fileId()
        ));
        return toPartPreviewResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)).preview());
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/processing", description = "Part 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/preview/processing")
    public PartPreviewProcessingResponse getProcessing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartPreviewProcessingResponse(
                partPreviewProcessingQuery.get(new PartPreviewProcessingCondition(partNumber, revisionCode, null, null))
        );
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview", description = "Part 대표 미리보기를 해제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public ResponseEntity<Void> delete(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        clearPartPreviewUseCase.execute(new ClearPartPreviewCommand(partNumber, revisionCode, null, null));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/files/{previewFileId}", description = "대표 미리보기 전용 파일 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/preview/files/{previewFileId}")
    public ResponseEntity<Void> deletePreviewFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "미리보기 파일 ID")
            @PathVariable UUID previewFileId
    ) {
        deletePartPreviewFileUseCase.execute(new DeletePartPreviewFileCommand(
                partNumber,
                revisionCode,
                null,
                null,
                previewFileId
        ));
        return ResponseEntity.noContent().build();
    }
}
