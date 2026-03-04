package com.fabbitinc.server.presentation.organization.controller;

import com.fabbitinc.server.application.auth.dto.response.CreateOrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.LoginResponse;
import com.fabbitinc.server.application.organization.dto.request.CreateOrganizationRequest;
import com.fabbitinc.server.application.organization.dto.request.SetProfileImageRequest;
import com.fabbitinc.server.application.organization.dto.request.SwitchOrgRequest;
import com.fabbitinc.server.application.organization.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.organization.usecase.CreateOrganizationUseCase;
import com.fabbitinc.server.application.organization.usecase.DeleteOrganizationProfileImageUseCase;
import com.fabbitinc.server.application.organization.usecase.SetOrganizationProfileImageUseCase;
import com.fabbitinc.server.application.organization.usecase.SwitchOrganizationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations")
@Tag(name = "organizations", description = "조직 생성/전환/프로필 이미지 API")
public class OrganizationController {

    private final CreateOrganizationUseCase createOrganizationUseCase;
    private final SwitchOrganizationUseCase switchOrganizationUseCase;
    private final SetOrganizationProfileImageUseCase setProfileImageUseCase;
    private final DeleteOrganizationProfileImageUseCase deleteProfileImageUseCase;

    @Operation(
            summary = "POST /api/v1/organizations",
            description = "스코프 토큰(scope=create_org)으로 조직을 생성하고 access/refresh 토큰을 발급합니다"
    )
    @PostMapping
    public CreateOrganizationResponse createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        return createOrganizationUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/organizations/switch",
            description = "대상 워크스페이스 멤버십을 확인한 뒤 새 access/refresh 토큰을 발급합니다"
    )
    @PostMapping("/switch")
    public LoginResponse switchOrganization(
            @Valid @RequestBody SwitchOrgRequest request
    ) {
        return switchOrganizationUseCase.execute(request);
    }

    @Operation(
            summary = "PUT /api/v1/organizations/profile-image",
            description = "업로드 완료된 파일(file_id)을 조직 프로필 이미지로 설정합니다. ADMIN 이상 권한이 필요합니다"
    )
    @PutMapping("/profile-image")
    public ProfileImageResponse setProfileImage(
            @Valid @RequestBody SetProfileImageRequest request
    ) {
        return setProfileImageUseCase.execute(request.fileId());
    }

    @Operation(
            summary = "DELETE /api/v1/organizations/profile-image",
            description = "조직 프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다. ADMIN 이상 권한이 필요합니다"
    )
    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteProfileImage() {
        deleteProfileImageUseCase.execute();
        return ResponseEntity.noContent().build();
    }
}
