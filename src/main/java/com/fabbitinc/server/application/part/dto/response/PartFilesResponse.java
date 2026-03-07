package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record PartFilesResponse(
        long total,
        List<FileItemResponse> items
) {
}
