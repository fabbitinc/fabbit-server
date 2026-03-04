package com.fabbitinc.server.presentation.auth.controller;

import com.fabbitinc.server.application.auth.dto.request.SendVerificationRequest;
import com.fabbitinc.server.application.auth.dto.request.VerifyEmailRequest;
import com.fabbitinc.server.application.auth.dto.request.AcceptInvitationRequest;
import com.fabbitinc.server.application.auth.dto.request.LoginRequest;
import com.fabbitinc.server.application.auth.dto.request.RefreshRequest;
import com.fabbitinc.server.application.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.application.auth.dto.response.AcceptInvitationResponse;
import com.fabbitinc.server.application.auth.dto.response.CheckEmailResponse;
import com.fabbitinc.server.application.auth.dto.response.CheckSlugResponse;
import com.fabbitinc.server.application.auth.dto.response.LoginResponse;
import com.fabbitinc.server.application.auth.dto.response.PlanResponse;
import com.fabbitinc.server.application.auth.dto.response.RegisterResponse;
import com.fabbitinc.server.application.auth.dto.response.ScopedLoginResponse;
import com.fabbitinc.server.application.auth.dto.response.SendVerificationResponse;
import com.fabbitinc.server.application.auth.dto.response.SiteResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.VerifyEmailResponse;
import com.fabbitinc.server.application.auth.dto.response.VerifyInvitationResponse;
import com.fabbitinc.server.application.auth.query.AuthInvitationQuery;
import com.fabbitinc.server.application.auth.query.AuthQuery;
import com.fabbitinc.server.application.auth.usecase.AcceptInvitationUseCase;
import com.fabbitinc.server.application.auth.usecase.SendVerificationUseCase;
import com.fabbitinc.server.application.auth.usecase.VerifyEmailUseCase;
import com.fabbitinc.server.application.auth.usecase.LoginUseCase;
import com.fabbitinc.server.application.auth.usecase.LogoutUseCase;
import com.fabbitinc.server.application.auth.usecase.RegisterUseCase;
import com.fabbitinc.server.application.auth.usecase.RefreshTokenUseCase;
import com.fabbitinc.server.application.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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

import java.util.List;

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

    @Operation(summary = "GET /api/v1/auth/plans", description = "플랜 목록 조회")
    @GetMapping("/plans")
    public List<PlanResponse> getPlans() {
        return authQuery.getPlans();
    }

    @Operation(summary = "GET /api/v1/auth/check-slug", description = "워크스페이스 slug 중복/형식 검사")
    @GetMapping("/check-slug")
    public CheckSlugResponse checkSlug(
            @RequestParam("slug")
            @Size(min = 3, max = 50, message = "길이는 3~50자여야 합니다")
            String slug
    ) {
        return authQuery.checkSlug(slug);
    }

    @Operation(summary = "GET /api/v1/auth/check-email", description = "이메일 중복 확인")
    @GetMapping("/check-email")
    public CheckEmailResponse checkEmail(
            @RequestParam("email")
            @Email(message = "유효한 이메일 형식이 아닙니다")
            String email
    ) {
        return authQuery.checkEmail(email);
    }

    @Operation(summary = "GET /api/v1/auth/site", description = "Origin 기반 워크스페이스 정보 조회")
    @GetMapping("/site")
    public SiteResponse getSite(@RequestHeader(value = "Origin", required = false) String origin) {
        return authQuery.getSite(origin);
    }

    @Operation(summary = "POST /api/v1/auth/send-verification", description = "이메일 인증 코드 발송")
    @PostMapping("/send-verification")
    public SendVerificationResponse sendVerification(@Valid @RequestBody SendVerificationRequest request) {
        return sendVerificationUseCase.execute(request);
    }

    @Operation(summary = "POST /api/v1/auth/verify-email", description = "이메일 인증 코드 검증")
    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return verifyEmailUseCase.execute(request);
    }

    @Operation(summary = "POST /api/v1/auth/register", description = "회원가입")
    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerUseCase.execute(request);
    }

    @Operation(summary = "POST /api/v1/auth/login", description = "로그인")
    @PostMapping("/login")
    public Object login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "Origin", required = false) String origin
    ) {
        String slug = extractOriginSlug(origin);
        Object result = loginUseCase.execute(request, slug);
        if (result instanceof LoginResponse || result instanceof ScopedLoginResponse) {
            return result;
        }
        throw new IllegalStateException("지원하지 않는 로그인 응답 타입입니다");
    }

    @Operation(summary = "POST /api/v1/auth/refresh", description = "리프레시 토큰으로 액세스 토큰 재발급")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokenUseCase.execute(request);
    }

    @Operation(summary = "POST /api/v1/auth/logout", description = "리프레시 토큰 폐기")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshRequest request
    ) {
        logoutUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "GET /api/v1/auth/invitations/verify", description = "초대 토큰 검증")
    @GetMapping("/invitations/verify")
    public VerifyInvitationResponse verifyInvitation(@RequestParam("token") String token) {
        return authInvitationQuery.verifyInvitation(token);
    }

    @Operation(summary = "POST /api/v1/auth/accept-invitation", description = "조직 초대 수락")
    @PostMapping("/accept-invitation")
    public AcceptInvitationResponse acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return acceptInvitationUseCase.execute(request);
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
