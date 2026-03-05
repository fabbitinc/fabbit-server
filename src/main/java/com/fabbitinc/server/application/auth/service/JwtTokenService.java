package com.fabbitinc.server.application.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.JwtProperties;
import com.fabbitinc.server.domain.auth.model.RefreshToken;
import com.fabbitinc.server.domain.auth.repository.RefreshTokenRepository;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

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
        String refreshJti = UUID.randomUUID().toString();

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

        String refreshToken = JWT.create()
                .withIssuer(jwtProperties.issuer())
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("type", "REFRESH")
                .withClaim("jti", refreshJti)
                .withExpiresAt(Date.from(refreshExp))
                .sign(algorithm);

        refreshTokenRepository.save(new RefreshToken(userId, refreshJti, refreshExp));
        return new IssuedTokens(accessToken, refreshToken, "bearer");
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

        RefreshToken storedToken = refreshTokenRepository.findByTokenJti(jti).orElse(null);
        if (storedToken == null) {
            revokeAllUserTokensWithCommit(userId);
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰이 재사용되었습니다. 다시 로그인해주세요");
        }

        if (!storedToken.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰 사용자 정보가 일치하지 않습니다");
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "refresh 토큰이 만료되었습니다");
        }

        refreshTokenRepository.delete(storedToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        Membership membership = membershipRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "소속된 조직이 없습니다"));

        return issueTokenBundle(
                userId,
                email.isBlank() ? user.getEmail() : email,
                membership.getOrgId(),
                membership.getRole().name()
        );
    }

    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
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
        template.executeWithoutResult(status -> refreshTokenRepository.deleteByUserId(userId));
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
    }
}
