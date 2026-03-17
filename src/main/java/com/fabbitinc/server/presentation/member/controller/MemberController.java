package com.fabbitinc.server.presentation.member.controller;

import com.fabbitinc.server.presentation.member.dto.request.ChangeRoleRequest;
import com.fabbitinc.server.presentation.member.dto.request.ChangeSeatRequest;
import com.fabbitinc.server.presentation.member.dto.response.MemberListResponse;
import com.fabbitinc.server.presentation.member.dto.response.MemberLookupItemResponse;
import com.fabbitinc.server.presentation.member.dto.response.MemberLookupResponse;
import com.fabbitinc.server.presentation.member.dto.response.MemberSummaryResponse;
import com.fabbitinc.server.application.member.query.MemberQuery;
import com.fabbitinc.server.application.member.query.condition.MemberListCondition;
import com.fabbitinc.server.application.member.query.condition.MemberLookupCondition;
import com.fabbitinc.server.application.member.query.result.MemberListResult;
import com.fabbitinc.server.application.member.query.result.MemberLookupItemResult;
import com.fabbitinc.server.application.member.query.result.MemberLookupResult;
import com.fabbitinc.server.application.member.query.result.MemberSummaryResult;
import com.fabbitinc.server.application.member.usecase.ChangeMemberRoleUseCase;
import com.fabbitinc.server.application.member.usecase.ChangeMemberSeatUseCase;
import com.fabbitinc.server.application.member.usecase.RemoveMemberUseCase;
import com.fabbitinc.server.application.member.usecase.command.ChangeMemberRoleCommand;
import com.fabbitinc.server.application.member.usecase.command.ChangeMemberSeatCommand;
import com.fabbitinc.server.application.member.usecase.command.RemoveMemberCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(name = "members", description = "조직 멤버 조회/권한 관리 API")
public class MemberController {

    private final MemberQuery memberQuery;
    private final ChangeMemberRoleUseCase changeMemberRoleUseCase;
    private final ChangeMemberSeatUseCase changeMemberSeatUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;

    @Operation(
            summary = "GET /api/v1/members/lookup",
            description = "조직 멤버 lookup 목록을 조회합니다 (autocomplete/picker 용도)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/lookup")
    public MemberLookupResponse lookupMembers(
            @Parameter(description = "이름 검색 (ILIKE)") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수") @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        MemberLookupResult result = memberQuery.lookup(new MemberLookupCondition(search, limit));
        return new MemberLookupResponse(
                result.items().stream().map(this::toLookupItemResponse).toList()
        );
    }

    @Operation(
            summary = "GET /api/v1/members",
            description = "현재 조직의 전체 멤버 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public MemberListResponse listMembers() {
        MemberListResult result = memberQuery.list(new MemberListCondition());
        return new MemberListResponse(
                result.items().stream().map(this::toMemberSummaryResponse).toList(),
                result.maxMembers()
        );
    }

    @Operation(
            summary = "PATCH /api/v1/members/{userId}/role",
            description = "소유자(OWNER) 권한으로 멤버 역할을 변경합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeMemberRole(
            @Parameter(description = "역할을 변경할 사용자 ID")
            @PathVariable UUID userId,
            @Parameter(description = "멤버 역할 변경 요청")
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        changeMemberRoleUseCase.execute(new ChangeMemberRoleCommand(userId, request.role()));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "멤버 좌석 변경",
            description = "관리자(ADMIN 이상) 권한으로 멤버 좌석 타입을 변경합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PatchMapping("/{userId}/seat")
    public ResponseEntity<Void> changeMemberSeat(
            @Parameter(description = "좌석을 변경할 사용자 ID")
            @PathVariable UUID userId,
            @Parameter(description = "멤버 좌석 변경 요청")
            @Valid @RequestBody ChangeSeatRequest request
    ) {
        changeMemberSeatUseCase.execute(new ChangeMemberSeatCommand(userId, request.seatType()));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "DELETE /api/v1/members/{userId}",
            description = "관리자(ADMIN 이상) 권한으로 조직 멤버를 제거합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "조직에서 제거할 사용자 ID")
            @PathVariable UUID userId
    ) {
        removeMemberUseCase.execute(new RemoveMemberCommand(userId));
        return ResponseEntity.noContent().build();
    }

    private MemberLookupItemResponse toLookupItemResponse(MemberLookupItemResult result) {
        return new MemberLookupItemResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private MemberSummaryResponse toMemberSummaryResponse(MemberSummaryResult result) {
        return new MemberSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl(),
                result.role(),
                result.jobRole(),
                result.seatType()
        );
    }
}
