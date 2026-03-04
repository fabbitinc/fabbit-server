package com.fabbitinc.server.application.project.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record LinkPartsRequest(
        @NotEmpty(message = "part_ids는 1개 이상이어야 합니다")
        List<UUID> partIds
) {
}
