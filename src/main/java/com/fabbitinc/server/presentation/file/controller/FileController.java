package com.fabbitinc.server.presentation.file.controller;

import com.fabbitinc.server.presentation.file.dto.request.BatchCompleteRequest;
import com.fabbitinc.server.presentation.file.dto.request.BatchCreateFileRequest;
import com.fabbitinc.server.presentation.file.dto.request.CreateFileRequest;
import com.fabbitinc.server.presentation.file.dto.response.BatchCompleteFailure;
import com.fabbitinc.server.presentation.file.dto.response.BatchCompleteResponse;
import com.fabbitinc.server.presentation.file.dto.response.BatchCreateFileResponse;
import com.fabbitinc.server.presentation.file.dto.response.CreateFileResponse;
import com.fabbitinc.server.presentation.file.dto.response.FileCompleteResponse;
import com.fabbitinc.server.application.file.usecase.BatchCompleteFilesUseCase;
import com.fabbitinc.server.application.file.usecase.BatchCreateFilesUseCase;
import com.fabbitinc.server.application.file.usecase.CompleteFileUseCase;
import com.fabbitinc.server.application.file.usecase.CreateFileUseCase;
import com.fabbitinc.server.application.file.usecase.command.BatchCompleteFilesCommand;
import com.fabbitinc.server.application.file.usecase.command.BatchCreateFilesCommand;
import com.fabbitinc.server.application.file.usecase.command.CompleteFileCommand;
import com.fabbitinc.server.application.file.usecase.command.CreateFileCommand;
import com.fabbitinc.server.application.file.usecase.result.BatchCompleteFailureResult;
import com.fabbitinc.server.application.file.usecase.result.BatchCompletedFilesResult;
import com.fabbitinc.server.application.file.usecase.result.BatchCreatedFilesResult;
import com.fabbitinc.server.application.file.usecase.result.CompletedFileResult;
import com.fabbitinc.server.application.file.usecase.result.CreatedFileResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "files", description = "파일 업로드/완료 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "처리 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class FileController {

    private final CreateFileUseCase createFileUseCase;
    private final BatchCreateFilesUseCase batchCreateFilesUseCase;
    private final BatchCompleteFilesUseCase batchCompleteFilesUseCase;
    private final CompleteFileUseCase completeFileUseCase;

    @Operation(
            summary = "POST /api/v1/files/upload",
            description = "단건 파일 업로드를 위한 presigned URL을 발급합니다"
    )
    @PostMapping("/upload")
    public CreateFileResponse createFile(
            @Parameter(description = "파일 업로드 URL 발급 요청")
            @Valid @RequestBody CreateFileRequest request
    ) {
        return toCreateFileResponse(createFileUseCase.execute(
                new CreateFileCommand(
                        request.originalName(),
                        request.contentType(),
                        request.fileSize(),
                        request.contentHash()
                )
        ));
    }

    @Operation(
            summary = "POST /api/v1/files/upload/batch",
            description = "최대 100건 파일 업로드를 위한 presigned URL을 일괄 발급합니다"
    )
    @PostMapping("/upload/batch")
    public BatchCreateFileResponse batchCreateFiles(
            @Parameter(description = "배치 파일 업로드 URL 발급 요청")
            @Valid @RequestBody BatchCreateFileRequest request
    ) {
        return toBatchCreateFileResponse(batchCreateFilesUseCase.execute(
                new BatchCreateFilesCommand(
                        request.items().stream()
                                .map(item -> new CreateFileCommand(
                                        item.originalName(),
                                        item.contentType(),
                                        item.fileSize(),
                                        item.contentHash()
                                ))
                                .toList()
                )
        ));
    }

    @Operation(
            summary = "POST /api/v1/files/upload/batch/complete",
            description = "배치 업로드 완료를 확인하고 성공/실패 목록을 반환합니다"
    )
    @PostMapping("/upload/batch/complete")
    public BatchCompleteResponse batchCompleteFiles(
            @Parameter(description = "배치 파일 업로드 완료 요청")
            @Valid @RequestBody BatchCompleteRequest request
    ) {
        return toBatchCompleteResponse(batchCompleteFilesUseCase.execute(
                new BatchCompleteFilesCommand(request.fileIds())
        ));
    }

    @Operation(
            summary = "POST /api/v1/files/upload/{fileId}/complete",
            description = "단건 업로드 완료를 확인하고 파일 상태를 UPLOADED로 전이합니다"
    )
    @PostMapping("/upload/{fileId}/complete")
    public FileCompleteResponse completeFile(
            @Parameter(description = "업로드 완료 처리할 파일 ID")
            @PathVariable UUID fileId
    ) {
        return toFileCompleteResponse(completeFileUseCase.execute(new CompleteFileCommand(fileId)));
    }

    private CreateFileResponse toCreateFileResponse(CreatedFileResult result) {
        return new CreateFileResponse(result.fileId(), result.uploadUrl(), result.fileKey());
    }

    private BatchCreateFileResponse toBatchCreateFileResponse(BatchCreatedFilesResult result) {
        return new BatchCreateFileResponse(
                result.items().stream()
                        .map(this::toCreateFileResponse)
                        .toList()
        );
    }

    private BatchCompleteResponse toBatchCompleteResponse(BatchCompletedFilesResult result) {
        return new BatchCompleteResponse(
                result.items().stream()
                        .map(this::toFileCompleteResponse)
                        .toList(),
                result.failed().stream()
                        .map(this::toBatchCompleteFailure)
                        .toList()
        );
    }

    private BatchCompleteFailure toBatchCompleteFailure(BatchCompleteFailureResult result) {
        return new BatchCompleteFailure(result.fileId(), result.reason());
    }

    private FileCompleteResponse toFileCompleteResponse(CompletedFileResult result) {
        return new FileCompleteResponse(
                result.fileId(),
                result.status(),
                result.originalName(),
                result.fileKey(),
                result.fileSize(),
                result.contentType(),
                result.createdAt()
        );
    }
}
