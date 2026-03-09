package com.fabbitinc.server.presentation.organization.controller;

import com.fabbitinc.server.presentation.auth.dto.response.CreateOrganizationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.LoginResponse;
import com.fabbitinc.server.presentation.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.TokenResponse;
import com.fabbitinc.server.presentation.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.organization.dto.request.CreateOrganizationRequest;
import com.fabbitinc.server.application.organization.dto.request.SetProfileImageRequest;
import com.fabbitinc.server.application.organization.dto.request.SwitchOrgRequest;
import com.fabbitinc.server.application.organization.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.organization.usecase.CreateOrganizationUseCase;
import com.fabbitinc.server.application.organization.usecase.DeleteOrganizationProfileImageUseCase;
import com.fabbitinc.server.application.organization.usecase.SetOrganizationProfileImageUseCase;
import com.fabbitinc.server.application.organization.usecase.SwitchOrganizationUseCase;
import com.fabbitinc.server.application.organization.usecase.command.CreateOrganizationCommand;
import com.fabbitinc.server.application.organization.usecase.command.DeleteOrganizationProfileImageCommand;
import com.fabbitinc.server.application.organization.usecase.command.SetOrganizationProfileImageCommand;
import com.fabbitinc.server.application.organization.usecase.command.SwitchOrganizationCommand;
import com.fabbitinc.server.application.organization.usecase.result.CreateOrganizationResult;
import com.fabbitinc.server.application.organization.usecase.result.SetOrganizationProfileImageResult;
import com.fabbitinc.server.application.organization.usecase.result.SwitchOrganizationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조직 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    public CreateOrganizationResponse createOrganization(
            @Parameter(description = "조직 생성 요청")
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        CreateOrganizationResult result = createOrganizationUseCase.execute(
                new CreateOrganizationCommand(
                        request.orgName(),
                        request.slug(),
                        request.industry(),
                        request.teamSize(),
                        request.planType()
                )
        );
        return new CreateOrganizationResponse(
                new OrganizationResponse(
                        result.organizationId(),
                        result.organizationSlug(),
                        result.organizationName(),
                        result.organizationIndustry(),
                        result.organizationTeamSize(),
                        result.organizationPlanType(),
                        result.organizationProfileImageUrl()
                ),
                new TokenResponse(result.accessToken(), result.refreshToken(), result.tokenType())
        );
    }

    @Operation(
            summary = "POST /api/v1/organizations/switch",
            description = "대상 워크스페이스 멤버십을 확인한 뒤 새 access/refresh 토큰을 발급합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "워크스페이스 전환 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/switch")
    public LoginResponse switchOrganization(
            @Parameter(description = "워크스페이스 전환 요청")
            @Valid @RequestBody SwitchOrgRequest request
    ) {
        SwitchOrganizationResult result = switchOrganizationUseCase.execute(
                new SwitchOrganizationCommand(request.slug())
        );
        return new LoginResponse(
                new UserResponse(
                        result.userId(),
                        result.userEmail(),
                        result.userFullName(),
                        result.userPhone(),
                        result.userProfileImageUrl(),
                        result.userActive(),
                        result.userCreatedAt()
                ),
                new TokenResponse(result.accessToken(), result.refreshToken(), result.tokenType())
        );
    }

    @Operation(
            summary = "PUT /api/v1/organizations/profile-image",
            description = "업로드 완료된 파일(file_id)을 조직 프로필 이미지로 설정합니다. ADMIN 이상 권한이 필요합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PutMapping("/profile-image")
    public ProfileImageResponse setProfileImage(
            @Parameter(description = "조직 프로필 이미지 설정 요청")
            @Valid @RequestBody SetProfileImageRequest request
    ) {
        SetOrganizationProfileImageResult result = setProfileImageUseCase.execute(
                new SetOrganizationProfileImageCommand(request.fileId())
        );
        return new ProfileImageResponse(result.profileImageUrl());
    }

    @Operation(
            summary = "DELETE /api/v1/organizations/profile-image",
            description = "조직 프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다. ADMIN 이상 권한이 필요합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "프로필 이미지 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteProfileImage() {
        deleteProfileImageUseCase.execute(new DeleteOrganizationProfileImageCommand());
        return ResponseEntity.noContent().build();
    }
}
