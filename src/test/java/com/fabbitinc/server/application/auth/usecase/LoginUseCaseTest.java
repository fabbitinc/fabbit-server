package com.fabbitinc.server.application.auth.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.service.AuthAccountService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.service.input.LoginInput;
import com.fabbitinc.server.application.auth.usecase.command.LoginCommand;
import com.fabbitinc.server.application.auth.usecase.result.LoginResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private AuthAccountService authAccountService;
    @Mock
    private OrganizationApi organizationApi;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private FileUrlResolver fileUrlResolver;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(
                authAccountService,
                organizationApi,
                jwtTokenService,
                fileUrlResolver
        );
    }

    @Test
    void execute_slug가_없고_소유한_워크스페이스가_없으면_조직생성용_스코프_토큰을_발급한다() {
        User user = User.create("new-user@example.com", "hashed-password", "New User");
        when(authAccountService.authenticate(any(LoginInput.class))).thenReturn(user);
        when(organizationApi.hasOwnedOrganization(user.getId())).thenReturn(false);
        when(jwtTokenService.issueScopedToken(user.getId(), user.getEmail(), "create_org"))
                .thenReturn("scoped-token");

        LoginResult result = loginUseCase.execute(new LoginCommand(user.getEmail(), "plain-password", null));

        assertTrue(result.scoped());
        assertEquals("scoped-token", result.scopedAccessToken());
        verify(organizationApi).hasOwnedOrganization(user.getId());
    }

    @Test
    void execute_slug가_없고_이미_소유한_워크스페이스가_있으면_조직생성용_스코프_토큰을_발급하지_않는다() {
        User user = User.create("member@example.com", "hashed-password", "Member");
        when(authAccountService.authenticate(any(LoginInput.class))).thenReturn(user);
        when(organizationApi.hasOwnedOrganization(user.getId())).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> loginUseCase.execute(new LoginCommand(user.getEmail(), "plain-password", null)));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("이미 생성한 워크스페이스가 있습니다. 해당 워크스페이스에서 로그인해주세요", exception.getMessage());
        verify(organizationApi).hasOwnedOrganization(user.getId());
        verifyNoInteractions(jwtTokenService);
    }
}
