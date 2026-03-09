package com.fabbitinc.server.presentation.user.controller;

import com.fabbitinc.server.presentation.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.user.dto.request.ChangePasswordRequest;
import com.fabbitinc.server.application.user.dto.request.SetProfileImageRequest;
import com.fabbitinc.server.application.user.dto.request.UpdateProfileRequest;
import com.fabbitinc.server.application.user.dto.response.MeResponse;
import com.fabbitinc.server.application.user.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.user.dto.response.UpdateProfileResponse;
import com.fabbitinc.server.application.user.dto.response.UserMembershipResponse;
import com.fabbitinc.server.application.user.query.UserQuery;
import com.fabbitinc.server.application.user.query.condition.MeCondition;
import com.fabbitinc.server.application.user.query.result.MeResult;
import com.fabbitinc.server.application.user.query.result.QueryOrganizationResult;
import com.fabbitinc.server.application.user.query.result.QueryUserResult;
import com.fabbitinc.server.application.user.query.result.UserMembershipResult;
import com.fabbitinc.server.application.user.usecase.ChangePasswordUseCase;
import com.fabbitinc.server.application.user.usecase.DeleteProfileImageUseCase;
import com.fabbitinc.server.application.user.usecase.SetProfileImageUseCase;
import com.fabbitinc.server.application.user.usecase.UpdateProfileUseCase;
import com.fabbitinc.server.application.user.usecase.command.ChangePasswordCommand;
import com.fabbitinc.server.application.user.usecase.command.DeleteUserProfileImageCommand;
import com.fabbitinc.server.application.user.usecase.command.SetUserProfileImageCommand;
import com.fabbitinc.server.application.user.usecase.command.UpdateProfileCommand;
import com.fabbitinc.server.application.user.usecase.result.SetUserProfileImageResult;
import com.fabbitinc.server.application.user.usecase.result.UpdateProfileResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "users", description = "내 정보 조회/수정 API")
public class UserController {

    private final UserQuery userQuery;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final SetProfileImageUseCase setProfileImageUseCase;
    private final DeleteProfileImageUseCase deleteProfileImageUseCase;

    @Operation(
            summary = "GET /api/v1/users/me",
            description = "현재 인증된 사용자의 프로필과 소속 조직 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/me")
    public MeResponse getMe() {
        MeResult result = userQuery.getMe(new MeCondition());
        return new MeResponse(
                toUserResponse(result.user()),
                result.memberships().stream().map(this::toMembershipResponse).toList()
        );
    }

    @Operation(
            summary = "PATCH /api/v1/users/me",
            description = "내 프로필을 부분 수정합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PatchMapping("/me")
    public UpdateProfileResponse updateProfile(
            @Parameter(description = "내 프로필 수정 요청")
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UpdateProfileResult result = updateProfileUseCase.execute(
                new UpdateProfileCommand(request.fullName(), request.phone())
        );
        return new UpdateProfileResponse(
                result.id(),
                result.email(),
                result.fullName(),
                result.phone(),
                result.profileImageUrl(),
                result.updatedAt()
        );
    }

    @Operation(
            summary = "PUT /api/v1/users/me/password",
            description = "현재 비밀번호를 검증한 후 새 비밀번호로 변경합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "비밀번호 변경 요청")
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(
                new ChangePasswordCommand(request.currentPassword(), request.newPassword())
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "PUT /api/v1/users/me/profile-image",
            description = "업로드 완료된 파일(file_id)을 사용자 프로필 이미지로 설정합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PutMapping("/me/profile-image")
    public ProfileImageResponse setProfileImage(
            @Parameter(description = "프로필 이미지 설정 요청")
            @Valid @RequestBody SetProfileImageRequest request
    ) {
        SetUserProfileImageResult result = setProfileImageUseCase.execute(
                new SetUserProfileImageCommand(request.fileId())
        );
        return new ProfileImageResponse(result.profileImageUrl());
    }

    @Operation(
            summary = "DELETE /api/v1/users/me/profile-image",
            description = "사용자 프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<Void> deleteProfileImage() {
        deleteProfileImageUseCase.execute(new DeleteUserProfileImageCommand());
        return ResponseEntity.noContent().build();
    }

    private UserResponse toUserResponse(QueryUserResult user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.fullName(),
                user.phone(),
                user.profileImageUrl(),
                user.active(),
                user.createdAt()
        );
    }

    private UserMembershipResponse toMembershipResponse(UserMembershipResult membership) {
        return new UserMembershipResponse(
                membership.orgId(),
                membership.role(),
                membership.jobRole(),
                toOrganizationResponse(membership.organization())
        );
    }

    private OrganizationResponse toOrganizationResponse(QueryOrganizationResult organization) {
        return new OrganizationResponse(
                organization.id(),
                organization.slug(),
                organization.name(),
                organization.industry(),
                organization.teamSize(),
                organization.planType(),
                organization.profileImageUrl()
        );
    }
}
