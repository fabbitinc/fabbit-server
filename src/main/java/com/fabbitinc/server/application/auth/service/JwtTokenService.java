package com.fabbitinc.server.application.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.JwtProperties;
import com.fabbitinc.server.domain.auth.model.RefreshToken;
import com.fabbitinc.server.domain.auth.repository.RefreshTokenRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PlatformTransactionManager transactionManager;

    public IssuedTokens issueTokenBundle(UUID userId, String email, UUID orgId, String role) {
        Instant now = Instant.now();
        Instant accessExp = now.plus(jwtProperties.accessTokenExpireMinutes(), ChronoUnit.MINUTES);
        Instant refreshExp = now.plus(jwtProperties.refreshTokenExpireDays(), ChronoUnit.DAYS);
        RefreshToken refreshToken = RefreshToken.create(userId, UUID.randomUUID().toString(), refreshExp);

        refreshTokenRepository.save(refreshToken);
        return buildIssuedTokens(userId, email, orgId, role, accessExp, refreshToken);
    }

    public String issueScopedToken(UUID userId, String email, String scope) {
        Instant exp = Instant.now().plus(jwtProperties.accessTokenExpireMinutes(), ChronoUnit.MINUTES);
        return JWT.create()
                .withIssuer(jwtProperties.issuer())
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("type", "SCOPED")
                .withClaim("scope", scope)
                .withExpiresAt(Date.from(exp))
                .sign(algorithm());
    }

    public IssuedTokens refreshTokenBundle(String refreshToken) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);

        String jti = requiredClaim(decoded, "jti");
        UUID userId = parseUuid(decoded.getSubject(), "유효하지 않은 사용자 토큰입니다");
        String email = requiredClaim(decoded, "email");
        Instant now = Instant.now();

        RefreshToken storedToken = refreshTokenRepository.findByTokenJti(jti).orElse(null);
        if (storedToken == null) {
            revokeAllUserTokensWithCommit(userId);
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰이 재사용되었습니다. 다시 로그인해주세요");
        }

        try {
            storedToken.validateOwnedBy(userId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, ex.getMessage());
        }

        try {
            storedToken.validateUsableAt(now);
        } catch (DomainException ex) {
            if (RefreshToken.CODE_REFRESH_TOKEN_REVOKED.equals(ex.getDomainCode())) {
                revokeAllUserTokensWithCommit(userId);
                throw new AppException(ErrorCode.TOKEN_INVALID, ex.getMessage());
            }
            if (RefreshToken.CODE_REFRESH_TOKEN_EXPIRED.equals(ex.getDomainCode())) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED, ex.getMessage());
            }
            throw new AppException(ErrorCode.TOKEN_INVALID, ex.getMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        Membership membership = membershipRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "소속된 조직이 없습니다"));

        Instant accessExp = now.plus(jwtProperties.accessTokenExpireMinutes(), ChronoUnit.MINUTES);
        Instant refreshExp = now.plus(jwtProperties.refreshTokenExpireDays(), ChronoUnit.DAYS);
        RefreshToken rotatedToken = storedToken.rotate(UUID.randomUUID().toString(), refreshExp, now);
        refreshTokenRepository.save(storedToken);
        refreshTokenRepository.save(rotatedToken);

        return buildIssuedTokens(
                userId,
                email.isBlank() ? user.getEmail() : email,
                membership.getOrgId(),
                membership.getRole().name(),
                accessExp,
                rotatedToken
        );
    }

    public void revokeAllUserTokens(UUID userId) {
        revokeAllUserTokensAt(userId, Instant.now());
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(jwtProperties.secretKey());
    }

    private DecodedJWT verifyRefreshToken(String refreshToken) {
        try {
            DecodedJWT decoded = JWT.require(algorithm())
                    .withIssuer(jwtProperties.issuer())
                    .build()
                    .verify(refreshToken);

            String tokenType = decoded.getClaim("type").asString();
            if (!"REFRESH".equals(tokenType)) {
                throw new AppException(ErrorCode.TOKEN_INVALID, "refresh 토큰이 아닙니다");
            }
            return decoded;
        } catch (TokenExpiredException ex) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "refresh 토큰이 만료되었습니다");
        } catch (JWTVerificationException ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "유효하지 않은 refresh 토큰입니다");
        }
    }

    private String requiredClaim(DecodedJWT decoded, String claimName) {
        String value = decoded.getClaim(claimName).asString();
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰 claim(" + claimName + ")이 비어 있습니다");
        }
        return value;
    }

    private UUID parseUuid(String raw, String message) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, message);
        }
    }

    private void revokeAllUserTokensWithCommit(UUID userId) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> revokeAllUserTokensAt(userId, Instant.now()));
    }

    private void revokeAllUserTokensAt(UUID userId, Instant revokedAt) {
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(token -> token.revoke(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private IssuedTokens buildIssuedTokens(
            UUID userId,
            String email,
            UUID orgId,
            String role,
            Instant accessExp,
            RefreshToken refreshToken
    ) {
        Algorithm algorithm = algorithm();
        String accessToken = JWT.create()
                .withIssuer(jwtProperties.issuer())
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("orgId", orgId.toString())
                .withClaim("role", role)
                .withClaim("type", "ACCESS")
                .withExpiresAt(Date.from(accessExp))
                .sign(algorithm);

        String refreshTokenValue = JWT.create()
                .withIssuer(jwtProperties.issuer())
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("type", "REFRESH")
                .withClaim("jti", refreshToken.getTokenJti())
                .withExpiresAt(Date.from(refreshToken.getExpiresAt()))
                .sign(algorithm);

        return new IssuedTokens(accessToken, refreshTokenValue, "bearer");
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
    }
}
