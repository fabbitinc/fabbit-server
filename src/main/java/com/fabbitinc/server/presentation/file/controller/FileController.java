package com.fabbitinc.server.presentation.file.controller;

import com.fabbitinc.server.application.file.dto.request.BatchCompleteRequest;
import com.fabbitinc.server.application.file.dto.request.BatchCreateFileRequest;
import com.fabbitinc.server.application.file.dto.request.CreateFileRequest;
import com.fabbitinc.server.application.file.dto.response.BatchCompleteResponse;
import com.fabbitinc.server.application.file.dto.response.BatchCreateFileResponse;
import com.fabbitinc.server.application.file.dto.response.CreateFileResponse;
import com.fabbitinc.server.application.file.dto.response.FileCompleteResponse;
import com.fabbitinc.server.application.file.usecase.BatchCompleteFilesUseCase;
import com.fabbitinc.server.application.file.usecase.BatchCreateFilesUseCase;
import com.fabbitinc.server.application.file.usecase.CompleteFileUseCase;
import com.fabbitinc.server.application.file.usecase.CreateFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "files", description = "파일 업로드/완료 API")
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
            @Valid @RequestBody CreateFileRequest request
    ) {
        return createFileUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/files/upload/batch",
            description = "최대 100건 파일 업로드를 위한 presigned URL을 일괄 발급합니다"
    )
    @PostMapping("/upload/batch")
    public BatchCreateFileResponse batchCreateFiles(
            @Valid @RequestBody BatchCreateFileRequest request
    ) {
        return batchCreateFilesUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/files/upload/batch/complete",
            description = "배치 업로드 완료를 확인하고 성공/실패 목록을 반환합니다"
    )
    @PostMapping("/upload/batch/complete")
    public BatchCompleteResponse batchCompleteFiles(
            @Valid @RequestBody BatchCompleteRequest request
    ) {
        return batchCompleteFilesUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/files/upload/{fileId}/complete",
            description = "단건 업로드 완료를 확인하고 파일 상태를 UPLOADED로 전이합니다"
    )
    @PostMapping("/upload/{fileId}/complete")
    public FileCompleteResponse completeFile(
            @PathVariable UUID fileId
    ) {
        return completeFileUseCase.execute(fileId);
    }
}
