package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;

import java.util.List;

public record PartFilesResponse(
        long total,
        List<FileItemResponse> items
) {
}
