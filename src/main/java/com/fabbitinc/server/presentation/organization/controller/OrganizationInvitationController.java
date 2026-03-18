package com.fabbitinc.server.presentation.organization.controller;

import com.fabbitinc.server.presentation.auth.dto.request.CreateInvitationRequest;
import com.fabbitinc.server.presentation.auth.dto.response.InvitationListResponse;
import com.fabbitinc.server.presentation.auth.dto.response.InvitationResponse;
import com.fabbitinc.server.application.organization.query.OrganizationInvitationQuery;
import com.fabbitinc.server.application.organization.query.result.OrganizationInvitationListResult;
import com.fabbitinc.server.application.organization.usecase.CancelInvitationUseCase;
import com.fabbitinc.server.application.organization.usecase.CreateInvitationUseCase;
import com.fabbitinc.server.application.organization.usecase.command.CancelInvitationCommand;
import com.fabbitinc.server.application.organization.usecase.command.CreateInvitationCommand;
import com.fabbitinc.server.application.organization.usecase.result.CreateInvitationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/invitations")
@Tag(name = "organizations", description = "조직 초대 API")
public class OrganizationInvitationController {

    private final CreateInvitationUseCase createInvitationUseCase;
    private final OrganizationInvitationQuery organizationInvitationQuery;
    private final CancelInvitationUseCase cancelInvitationUseCase;

    @Operation(
            summary = "관리자(ADMIN 이상)가 이메일로 조직 초대를 발송합니다",
            description = "관리자(ADMIN 이상)가 이메일로 조직 초대를 발송합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "초대 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @Parameter(description = "조직 초대 생성 요청")
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        CreateInvitationResult result = createInvitationUseCase.execute(
                new CreateInvitationCommand(request.email(), request.role(), request.seatType())
        );
        InvitationResponse response = new InvitationResponse(
                result.id(),
                result.orgId(),
                result.email(),
                result.role(),
                result.seatType(),
                result.status(),
                result.invitedBy(),
                result.expiresAt(),
                result.acceptedAt(),
                result.createdAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "관리자(ADMIN 이상)가 조직의 초대 목록(PENDING/ACCEPTED/CANCELLED)을 최신순으로 조회합니다",
            description = "관리자(ADMIN 이상)가 조직의 초대 목록(PENDING/ACCEPTED/CANCELLED)을 최신순으로 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public InvitationListResponse listInvitations() {
        OrganizationInvitationListResult result = organizationInvitationQuery.listInvitations();
        return new InvitationListResponse(
                result.invitations().stream()
                        .map(invitation -> new InvitationResponse(
                                invitation.id(),
                                invitation.orgId(),
                                invitation.email(),
                                invitation.role(),
                                invitation.seatType(),
                                invitation.status(),
                                invitation.invitedBy(),
                                invitation.expiresAt(),
                                invitation.acceptedAt(),
                                invitation.createdAt()
                        ))
                        .toList()
        );
    }

    @Operation(
            summary = "관리자(ADMIN 이상)가 PENDING 상태 초대를 취소합니다",
            description = "관리자(ADMIN 이상)가 PENDING 상태 초대를 취소합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "초대 취소 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @Parameter(description = "취소할 초대 ID")
            @PathVariable UUID invitationId
    ) {
        cancelInvitationUseCase.execute(new CancelInvitationCommand(invitationId));
        return ResponseEntity.noContent().build();
    }
}
