package com.fabbitinc.server.presentation.organization.controller;

import com.fabbitinc.server.application.auth.dto.request.CreateInvitationRequest;
import com.fabbitinc.server.application.auth.dto.response.InvitationListResponse;
import com.fabbitinc.server.application.auth.dto.response.InvitationResponse;
import com.fabbitinc.server.application.organization.query.OrganizationInvitationQuery;
import com.fabbitinc.server.application.organization.usecase.CancelInvitationUseCase;
import com.fabbitinc.server.application.organization.usecase.CreateInvitationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/invitations")
@Tag(name = "organizations", description = "조직 초대 API")
public class OrganizationInvitationController {

    private final CreateInvitationUseCase createInvitationUseCase;
    private final OrganizationInvitationQuery organizationInvitationQuery;
    private final CancelInvitationUseCase cancelInvitationUseCase;

    @Operation(
            summary = "POST /api/v1/organizations/invitations",
            description = "관리자(ADMIN 이상)가 이메일로 조직 초대를 발송합니다"
    )
    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        InvitationResponse response = createInvitationUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "GET /api/v1/organizations/invitations",
            description = "관리자(ADMIN 이상)가 조직의 초대 목록(PENDING/ACCEPTED/CANCELLED)을 최신순으로 조회합니다"
    )
    @GetMapping
    public InvitationListResponse listInvitations() {
        return organizationInvitationQuery.listInvitations();
    }

    @Operation(
            summary = "DELETE /api/v1/organizations/invitations/{invitation_id}",
            description = "관리자(ADMIN 이상)가 PENDING 상태 초대를 취소합니다"
    )
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable UUID invitationId
    ) {
        cancelInvitationUseCase.execute(invitationId);
        return ResponseEntity.noContent().build();
    }
}
