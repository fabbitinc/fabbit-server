package com.fabbitinc.server.presentation.auth.dto.request;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "이메일 인증 완료 후 발급된 verification token", example = "e9b6c2a9c0d24f9b8d99f5f5d67f73be")
        @NotBlank(message = "verification_token은 필수입니다") String verificationToken,

        @Schema(description = "이메일 인증코드", example = "123456")
        @NotBlank(message = "code는 필수입니다") @Size(min = 6, max = 6, message = "code는 6자리여야 합니다") String code,

        @Schema(description = "회원 비밀번호", example = "StrongPass123!")
        @NotBlank(message = "password는 필수입니다") @Size(min = 8, max = 128, message = "password 길이는 8~128자여야 합니다") String password,

        @Schema(description = "회원 이름", example = "홍길동")
        @NotBlank(message = "full_name은 필수입니다") @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다") String fullName,

        @Schema(description = "조직 이름", example = "Fabbit")
        @NotBlank(message = "org_name은 필수입니다") @Size(min = 1, max = 100, message = "org_name 길이는 1~100자여야 합니다") String orgName,

        @Schema(description = "워크스페이스 slug (미입력 시 자동 생성)", example = "fabbit")
        @Size(min = 3, max = 50, message = "slug 길이는 3~50자여야 합니다") String slug,

        @Schema(description = "업종", example = "software")
        String industry,

        @Schema(description = "팀 규모", example = "11-50")
        String teamSize,

        @Schema(
                description = "워크스페이스 시작 플랜 타입, 현재 가입 흐름에서는 Starter와 Team만 선택 가능",
                example = "STARTER",
                allowableValues = {"STARTER", "TEAM"}
        )
        @NotNull(message = "plan_type은 필수입니다") WorkspacePlanType planType,

        @Schema(
                description = "유료 플랜 선택 시 생성자에게 즉시 배정할 좌석 타입",
                example = "FULL",
                allowableValues = {"VIEWER", "COLLABORATOR", "FULL"}
        )
        SeatType ownerSeatType,

        @Schema(description = "봇 방지 토큰(선택)", example = "turnstile-token")
        String turnstileToken
) {
}
