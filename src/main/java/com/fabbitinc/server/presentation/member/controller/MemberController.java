package com.fabbitinc.server.presentation.member.controller;

import com.fabbitinc.server.application.member.dto.request.ChangeRoleRequest;
import com.fabbitinc.server.application.member.dto.response.MemberListResponse;
import com.fabbitinc.server.application.member.dto.response.MemberLookupResponse;
import com.fabbitinc.server.application.member.query.MemberQuery;
import com.fabbitinc.server.application.member.usecase.ChangeMemberRoleUseCase;
import com.fabbitinc.server.application.member.usecase.RemoveMemberUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(name = "members", description = "조직 멤버 조회/권한 관리 API")
public class MemberController {

    private final MemberQuery memberQuery;
    private final ChangeMemberRoleUseCase changeMemberRoleUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;

    @Operation(
            summary = "GET /api/v1/members/lookup",
            description = "조직 멤버 lookup 목록을 조회합니다 (autocomplete/picker 용도)"
    )
    @GetMapping("/lookup")
    public MemberLookupResponse lookupMembers(
            @Parameter(description = "이름 검색 (ILIKE)") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수") @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return memberQuery.lookupMembers(search, limit);
    }

    @Operation(
            summary = "GET /api/v1/members",
            description = "현재 조직의 전체 멤버 목록을 조회합니다"
    )
    @GetMapping
    public MemberListResponse listMembers() {
        return memberQuery.listOrgMembers();
    }

    @Operation(
            summary = "PATCH /api/v1/members/{user_id}/role",
            description = "소유자(OWNER) 권한으로 멤버 역할을 변경합니다"
    )
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeMemberRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        changeMemberRoleUseCase.execute(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "DELETE /api/v1/members/{user_id}",
            description = "관리자(ADMIN 이상) 권한으로 조직 멤버를 제거합니다"
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID userId
    ) {
        removeMemberUseCase.execute(userId);
        return ResponseEntity.noContent().build();
    }
}
