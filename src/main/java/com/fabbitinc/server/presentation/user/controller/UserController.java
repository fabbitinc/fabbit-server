package com.fabbitinc.server.presentation.user.controller;

import com.fabbitinc.server.application.user.dto.request.ChangePasswordRequest;
import com.fabbitinc.server.application.user.dto.request.SetProfileImageRequest;
import com.fabbitinc.server.application.user.dto.request.UpdateProfileRequest;
import com.fabbitinc.server.application.user.dto.response.MeResponse;
import com.fabbitinc.server.application.user.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.user.dto.response.UpdateProfileResponse;
import com.fabbitinc.server.application.user.query.UserQuery;
import com.fabbitinc.server.application.user.usecase.ChangePasswordUseCase;
import com.fabbitinc.server.application.user.usecase.DeleteProfileImageUseCase;
import com.fabbitinc.server.application.user.usecase.SetProfileImageUseCase;
import com.fabbitinc.server.application.user.usecase.UpdateProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    @GetMapping("/me")
    public MeResponse getMe(@RequestHeader("Authorization") String authorizationHeader) {
        return userQuery.getMe(authorizationHeader);
    }

    @Operation(
            summary = "PATCH /api/v1/users/me",
            description = "내 프로필을 부분 수정합니다"
    )
    @PatchMapping("/me")
    public UpdateProfileResponse updateProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return updateProfileUseCase.execute(authorizationHeader, request);
    }

    @Operation(
            summary = "PUT /api/v1/users/me/password",
            description = "현재 비밀번호를 검증한 후 새 비밀번호로 변경합니다"
    )
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(authorizationHeader, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "PUT /api/v1/users/me/profile-image",
            description = "업로드 완료된 파일(file_id)을 사용자 프로필 이미지로 설정합니다"
    )
    @PutMapping("/me/profile-image")
    public ProfileImageResponse setProfileImage(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody SetProfileImageRequest request
    ) {
        return setProfileImageUseCase.execute(authorizationHeader, request.fileId());
    }

    @Operation(
            summary = "DELETE /api/v1/users/me/profile-image",
            description = "사용자 프로필 이미지를 제거하고 연결된 파일을 소프트 삭제합니다"
    )
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<Void> deleteProfileImage(@RequestHeader("Authorization") String authorizationHeader) {
        deleteProfileImageUseCase.execute(authorizationHeader);
        return ResponseEntity.noContent().build();
    }
}
