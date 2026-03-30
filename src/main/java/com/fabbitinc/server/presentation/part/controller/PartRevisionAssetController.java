package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartFilesResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toRegisterDrawingResponse;

import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.usecase.AttachPartFilesUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
import com.fabbitinc.server.presentation.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.presentation.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.presentation.part.request.AttachFilesRequest;
import com.fabbitinc.server.presentation.part.response.PartAttachmentItemResponse;
import com.fabbitinc.server.presentation.part.response.PartFilesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revision-assets", description = "부품 리비전 파일/도면 자산 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionAssetController {

    private final PartQuery partQuery;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;

    @Operation(operationId = "partRevisionAssetGetFiles", summary = "연결된 파일 목록을 조회합니다", description = "리비전에 연결된 파일과 도면 목록을 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/files")
    public PartFilesResponse getFiles(@PathVariable UUID partId, @PathVariable UUID revisionId) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partId, revisionId)));
    }

    @Operation(operationId = "partRevisionAssetAttachFiles", summary = "파일을 첨부합니다", description = "업로드 완료 파일들을 리비전에 배치 연결합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/files")
    public List<PartAttachmentItemResponse> attachFiles(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partId, revisionId, request.fileIds())
        );
        return partQuery.getFiles(new FileItemsCondition(result.fileIds())).stream()
                .map(PartResponseMapper::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(operationId = "partRevisionAssetDeleteFile", summary = "첨부 파일을 제거합니다", description = "리비전에 연결된 첨부 파일 1건을 제거합니다")
    @DeleteMapping("/{partId}/revisions/{revisionId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partId, revisionId, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "partRevisionAssetCreateDrawing", summary = "도면을 등록합니다", description = "업로드 완료 파일을 Drawing으로 등록하고 리비전에 연결합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/drawings")
    public RegisterDrawingResponse createDrawing(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(
                registerPartDrawingUseCase.execute(new RegisterPartDrawingCommand(partId, revisionId, request.fileId()))
        );
    }

    @Operation(operationId = "partRevisionAssetDeleteDrawing", summary = "도면을 삭제합니다", description = "리비전에 연결된 도면 1건을 삭제합니다")
    @DeleteMapping("/{partId}/revisions/{revisionId}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawing(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partId, revisionId, drawingId));
        return ResponseEntity.noContent().build();
    }
}
