package com.fabbitinc.server.presentation.auth.controller;

import com.fabbitinc.server.application.auth.query.AuthInvitationQuery;
import com.fabbitinc.server.application.auth.query.AuthQuery;
import com.fabbitinc.server.application.auth.query.condition.CheckEmailCondition;
import com.fabbitinc.server.application.auth.query.condition.CheckSlugCondition;
import com.fabbitinc.server.application.auth.query.condition.SiteCondition;
import com.fabbitinc.server.application.auth.query.condition.VerifyInvitationCondition;
import com.fabbitinc.server.application.auth.query.result.CheckEmailResult;
import com.fabbitinc.server.application.auth.query.result.CheckSlugResult;
import com.fabbitinc.server.application.auth.query.result.PlanResult;
import com.fabbitinc.server.application.auth.query.result.SiteResult;
import com.fabbitinc.server.application.auth.query.result.VerifyInvitationResult;
import com.fabbitinc.server.application.auth.usecase.AcceptInvitationUseCase;
import com.fabbitinc.server.application.auth.usecase.LoginUseCase;
import com.fabbitinc.server.application.auth.usecase.LogoutUseCase;
import com.fabbitinc.server.application.auth.usecase.RefreshTokenUseCase;
import com.fabbitinc.server.application.auth.usecase.RegisterUseCase;
import com.fabbitinc.server.application.auth.usecase.SendVerificationUseCase;
import com.fabbitinc.server.application.auth.usecase.VerifyEmailUseCase;
import com.fabbitinc.server.application.auth.usecase.command.AcceptInvitationCommand;
import com.fabbitinc.server.application.auth.usecase.command.LoginCommand;
import com.fabbitinc.server.application.auth.usecase.command.LogoutCommand;
import com.fabbitinc.server.application.auth.usecase.command.RefreshTokenCommand;
import com.fabbitinc.server.application.auth.usecase.command.RegisterCommand;
import com.fabbitinc.server.application.auth.usecase.command.SendVerificationCommand;
import com.fabbitinc.server.application.auth.usecase.command.VerifyEmailCommand;
import com.fabbitinc.server.application.auth.usecase.result.AcceptInvitationResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthOrganizationResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthUserResult;
import com.fabbitinc.server.application.auth.usecase.result.LoginResult;
import com.fabbitinc.server.application.auth.usecase.result.RefreshTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.RegisterResult;
import com.fabbitinc.server.application.auth.usecase.result.SendVerificationResult;
import com.fabbitinc.server.application.auth.usecase.result.VerifyEmailResult;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.presentation.auth.dto.request.AcceptInvitationRequest;
import com.fabbitinc.server.presentation.auth.dto.request.LoginRequest;
import com.fabbitinc.server.presentation.auth.dto.request.RefreshRequest;
import com.fabbitinc.server.presentation.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.presentation.auth.dto.request.SendVerificationRequest;
import com.fabbitinc.server.presentation.auth.dto.request.VerifyEmailRequest;
import com.fabbitinc.server.presentation.auth.dto.response.AcceptInvitationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.CheckEmailResponse;
import com.fabbitinc.server.presentation.auth.dto.response.CheckSlugResponse;
import com.fabbitinc.server.presentation.auth.dto.response.LoginResponse;
import com.fabbitinc.server.presentation.auth.dto.response.LoginVariantResponse;
import com.fabbitinc.server.presentation.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.PlanResponse;
import com.fabbitinc.server.presentation.auth.dto.response.RegisterResponse;
import com.fabbitinc.server.presentation.auth.dto.response.ScopedLoginResponse;
import com.fabbitinc.server.presentation.auth.dto.response.SendVerificationResponse;
import com.fabbitinc.server.presentation.auth.dto.response.SiteResponse;
import com.fabbitinc.server.presentation.auth.dto.response.TokenResponse;
import com.fabbitinc.server.presentation.auth.dto.response.UserResponse;
import com.fabbitinc.server.presentation.auth.dto.response.VerifyEmailResponse;
import com.fabbitinc.server.presentation.auth.dto.response.VerifyInvitationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "auth", description = "인증/회원가입/초대 수락 API")
public class AuthController {

    private final AuthQuery authQuery;
    private final SendVerificationUseCase sendVerificationUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthInvitationQuery authInvitationQuery;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final AppProperties appProperties;

    @Operation(
            operationId = "authGetPlans",
            summary = "플랜 목록 조회",
            description = "워크스페이스 시작 플랜 목록과 좌석 단가, 멤버 정책, 스토리지 기본 정책, AI 과금 모드, 현재 가입 가능 여부를 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/plans")
    public List<PlanResponse> getPlans() {
        return authQuery.listPlans().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Operation(operationId = "authCheckSlug", summary = "워크스페이스 slug 중복/형식 검사", description = "워크스페이스 slug 중복/형식 검사")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검사 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/check-slug")
    public CheckSlugResponse checkSlug(
            @Parameter(description = "검사할 워크스페이스 slug", example = "fabbit")
            @RequestParam("slug")
            @Size(min = 3, max = 50, message = "길이는 3~50자여야 합니다") String slug
    ) {
        CheckSlugResult result = authQuery.getSlugAvailability(new CheckSlugCondition(slug));
        return new CheckSlugResponse(result.available(), result.message(), result.suggestion());
    }

    @Operation(operationId = "authCheckEmail", summary = "이메일 중복 확인", description = "이메일 중복 확인")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검사 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/check-email")
    public CheckEmailResponse checkEmail(
            @Parameter(description = "검사할 이메일", example = "user@example.com")
            @RequestParam("email")
            @Email(message = "유효한 이메일 형식이 아닙니다") String email
    ) {
        CheckEmailResult result = authQuery.getEmailAvailability(new CheckEmailCondition(email));
        return new CheckEmailResponse(result.available(), result.message());
    }

    @Operation(operationId = "authGetSite", summary = "Origin 기반 워크스페이스 정보 조회", description = "Origin 기반 워크스페이스 정보 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/site")
    public SiteResponse getSite(
            @Parameter(description = "요청 Origin 헤더", example = "https://fabbit.lvh.me")
            @RequestHeader(value = "Origin", required = false) String origin
    ) {
        SiteResult result = authQuery.getSite(new SiteCondition(origin));
        return new SiteResponse(result.slug(), result.name(), result.logoUrl());
    }

    @Operation(operationId = "authSendVerification", summary = "이메일 인증 코드 발송", description = "이메일 인증 코드 발송")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일"),
            @ApiResponse(responseCode = "429", description = "요청 제한")
    })
    @PostMapping("/send-verification")
    public SendVerificationResponse sendVerification(
            @Parameter(description = "인증코드 발송 요청")
            @Valid @RequestBody SendVerificationRequest request
    ) {
        SendVerificationResult result = sendVerificationUseCase.execute(
                new SendVerificationCommand(request.email(), request.turnstileToken())
        );
        return new SendVerificationResponse(result.message());
    }

    @Operation(operationId = "authVerifyEmail", summary = "이메일 인증 코드 검증", description = "이메일 인증 코드 검증")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(
            @Parameter(description = "이메일 인증코드 검증 요청")
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        VerifyEmailResult result = verifyEmailUseCase.execute(
                new VerifyEmailCommand(request.email(), request.code())
        );
        return new VerifyEmailResponse(result.verificationToken(), result.email());
    }

    @Operation(
            operationId = "authRegister",
            summary = "회원가입",
            description = "이메일 인증이 끝난 사용자가 워크스페이스를 생성하고 시작 플랜을 선택합니다. 현재 가입으로 시작할 수 있는 플랜은 Starter와 Team이며, 유료 플랜이면 ownerSeatType을 함께 지정해야 합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "중복 리소스")
    })
    @PostMapping("/register")
    public RegisterResponse register(
            @Parameter(description = "회원가입 요청")
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResult result = registerUseCase.execute(
                new RegisterCommand(
                        request.verificationToken(),
                        request.code(),
                        request.password(),
                        request.fullName(),
                        request.orgName(),
                        request.slug(),
                        request.industry(),
                        request.teamSize(),
                        request.planType(),
                        request.ownerSeatType()
                )
        );
        return new RegisterResponse(
                toUserResponse(result.user()),
                toOrganizationResponse(result.organization()),
                toTokenResponse(result.tokens())
        );
    }

    @Operation(operationId = "authLogin", summary = "로그인", description = "로그인")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/login")
    public LoginVariantResponse login(
            @Parameter(description = "로그인 요청")
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "요청 Origin 헤더 (워크스페이스 로그인 식별)", example = "https://fabbit.lvh.me")
            @RequestHeader(value = "Origin", required = false) String origin
    ) {
        String slug = extractOriginSlug(origin);
        LoginResult result = loginUseCase.execute(
                new LoginCommand(request.email(), request.password(), slug)
        );

        if (result.scoped()) {
            return new ScopedLoginResponse(
                    toUserResponse(result.user()),
                    result.scopedAccessToken()
            );
        }

        return new LoginResponse(
                toUserResponse(result.user()),
                toTokenResponse(result.tokens())
        );
    }

    @Operation(operationId = "authRefresh", summary = "리프레시 토큰으로 액세스 토큰 재발급", description = "리프레시 토큰으로 액세스 토큰 재발급")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/refresh")
    public TokenResponse refresh(
            @Parameter(description = "토큰 재발급 요청")
            @Valid @RequestBody RefreshRequest request
    ) {
        RefreshTokenResult result = refreshTokenUseCase.execute(
                new RefreshTokenCommand(request.refreshToken())
        );
        return toTokenResponse(result.tokens());
    }

    @Operation(operationId = "authLogout", summary = "리프레시 토큰 폐기", description = "리프레시 토큰 폐기")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "로그아웃 요청")
            @Valid @RequestBody RefreshRequest request
    ) {
        logoutUseCase.execute(new LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "authVerifyInvitation", summary = "초대 토큰 검증", description = "초대 토큰 검증")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/invitations/verify")
    public VerifyInvitationResponse verifyInvitation(
            @Parameter(description = "초대 토큰", example = "invitation-token")
            @RequestParam("token") String token
    ) {
        VerifyInvitationResult result = authInvitationQuery.getVerifiedInvitation(
                new VerifyInvitationCondition(token)
        );
        return new VerifyInvitationResponse(
                result.email(),
                result.organizationName(),
                result.inviterName(),
                result.role(),
                result.seatType(),
                result.existingUser(),
                result.expiresAt()
        );
    }

    @Operation(operationId = "authAcceptInvitation", summary = "조직 초대 수락", description = "조직 초대 수락")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수락 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/accept-invitation")
    public AcceptInvitationResponse acceptInvitation(
            @Parameter(description = "초대 수락 요청")
            @Valid @RequestBody AcceptInvitationRequest request
    ) {
        AcceptInvitationResult result = acceptInvitationUseCase.execute(
                new AcceptInvitationCommand(request.token(), request.password(), request.fullName())
        );
        return new AcceptInvitationResponse(
                toUserResponse(result.user()),
                toOrganizationResponse(result.organization()),
                toTokenResponse(result.tokens()),
                result.isNewUser()
        );
    }

    private UserResponse toUserResponse(AuthUserResult user) {
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

    private OrganizationResponse toOrganizationResponse(AuthOrganizationResult organization) {
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

    private TokenResponse toTokenResponse(AuthTokenResult token) {
        return new TokenResponse(
                token.accessToken(),
                token.refreshToken(),
                token.tokenType()
        );
    }

    private PlanResponse toPlanResponse(PlanResult result) {
        return new PlanResponse(
                result.planType(),
                result.displayName(),
                result.description(),
                result.maxMembers(),
                result.baseStorageBytes(),
                result.extraStorageBytesPerFullSeat(),
                result.allowStorageOverage(),
                result.availableForSignup(),
                result.availableForStarterUpgrade(),
                result.contactRequired(),
                result.starterMonthlyAiCredits(),
                result.aiBillingMode(),
                result.viewerMonthlyPrice(),
                result.collaboratorMonthlyPrice(),
                result.fullSeatMonthlyPrice(),
                result.storageOveragePricePerGb()
        );
    }

    private String extractOriginSlug(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }

        String host = origin;
        int schemeIndex = host.indexOf("://");
        if (schemeIndex >= 0) {
            host = host.substring(schemeIndex + 3);
        }
        int portIndex = host.indexOf(':');
        if (portIndex >= 0) {
            host = host.substring(0, portIndex);
        }

        String baseDomain = appProperties.baseDomain();
        if (host.equals(baseDomain) || host.equals("www." + baseDomain)) {
            return null;
        }
        String suffix = "." + baseDomain;
        if (host.endsWith(suffix)) {
            return host.substring(0, host.length() - suffix.length());
        }
        return null;
    }
}
