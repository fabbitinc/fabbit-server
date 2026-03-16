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
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "part-revision-assets", description = "공식 부품 리비전 파일 및 도면 자산 관리 API")
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

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/files", description = "Part에 연결된 업로드 완료 파일 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/files")
    public PartFilesResponse getFiles(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partNumber, revisionCode, null, null)));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/files", description = "업로드 완료 파일들을 Part에 배치 연결합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/files")
    public List<PartAttachmentItemResponse> attachFiles(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partNumber, revisionCode, null, null, request.fileIds())
        );
        return partQuery.getFiles(new FileItemsCondition(result.fileIds())).stream()
                .map(PartResponseMapper::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/files/{fileId}", description = "Part에 연결된 첨부파일 1건을 제거합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "파일 ID")
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partNumber, revisionCode, null, null, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings", description = "업로드 완료 파일을 Drawing으로 등록하고 PartRevision에 연결합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drawings")
    public RegisterDrawingResponse createDrawing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, revisionCode, null, request.fileId())
        ));
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}", description = "PartRevision에 연결된 도면 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "도면 ID")
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partNumber, revisionCode, null, null, drawingId));
        return ResponseEntity.noContent().build();
    }
}
