package com.fabbitinc.server.application.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.config.JwtProperties;
import com.fabbitinc.server.domain.auth.model.RefreshToken;
import com.fabbitinc.server.domain.auth.repository.RefreshTokenRepository;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                new JwtProperties("test-secret-key", "fabbit-test", 15, 7),
                refreshTokenRepository,
                userRepository,
                membershipRepository,
                transactionManager
        );
    }

    @Test
    void refreshTokenBundle_정상토큰이면_기존토큰을_폐기하고_새토큰을_저장한다() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User user = User.create("user@example.com", "hashed-password", "Tester");
        Membership membership = org.mockito.Mockito.mock(Membership.class);
        when(membership.getOrgId()).thenReturn(orgId);
        when(membership.getRole()).thenReturn(MembershipRole.ADMIN);

        List<RefreshToken> savedTokens = new ArrayList<>();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            savedTokens.add(token);
            return token;
        });

        JwtTokenService.IssuedTokens issued = jwtTokenService.issueTokenBundle(
                userId,
                user.getEmail(),
                orgId,
                MembershipRole.ADMIN.name()
        );
        RefreshToken storedToken = savedTokens.getFirst();

        savedTokens.clear();
        when(refreshTokenRepository.findByTokenJti(storedToken.getTokenJti())).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId(userId)).thenReturn(Optional.of(membership));

        JwtTokenService.IssuedTokens rotated = jwtTokenService.refreshTokenBundle(issued.refreshToken());

        assertNotNull(rotated.accessToken());
        assertNotNull(rotated.refreshToken());
        assertNotNull(storedToken.getRevokedAt());
        assertEquals(2, savedTokens.size());
        assertEquals(storedToken.getId(), savedTokens.get(0).getId());
        assertNotNull(savedTokens.get(0).getRevokedAt());
        assertNull(savedTokens.get(1).getRevokedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokeAllUserTokens_활성토큰만_폐기한다() {
        UUID userId = UUID.randomUUID();
        RefreshToken first = RefreshToken.create(userId, "jti-1", Instant.parse("2026-03-10T00:00:00Z"));
        RefreshToken second = RefreshToken.create(userId, "jti-2", Instant.parse("2026-03-11T00:00:00Z"));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(first, second));
        when(refreshTokenRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        jwtTokenService.revokeAllUserTokens(userId);

        ArgumentCaptor<List<RefreshToken>> captor = ArgumentCaptor.forClass(List.class);
        verify(refreshTokenRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertNotNull(first.getRevokedAt());
        assertNotNull(second.getRevokedAt());
    }
}
