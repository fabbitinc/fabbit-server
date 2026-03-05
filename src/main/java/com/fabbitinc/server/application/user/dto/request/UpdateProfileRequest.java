package com.fabbitinc.server.application.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Schema(description = "변경할 이름", example = "홍길동")
        @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다")
        String fullName,

        @Schema(description = "변경할 전화번호", example = "010-1234-5678")
        @Size(max = 20, message = "phone 길이는 최대 20자입니다")
        String phone
) {
}
