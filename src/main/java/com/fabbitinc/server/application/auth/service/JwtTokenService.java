package com.fabbitinc.server.application.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.JwtProperties;
import com.fabbitinc.server.domain.auth.model.RefreshToken;
import com.fabbitinc.server.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
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

    public TokenResponse issueTokens(UUID userId, String email, UUID orgId, String role) {
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
                .withClaim("orgId", orgId.toString())
                .withClaim("role", role)
                .withClaim("type", "REFRESH")
                .withClaim("jti", refreshJti)
                .withExpiresAt(Date.from(refreshExp))
                .sign(algorithm);

        refreshTokenRepository.save(new RefreshToken(userId, refreshJti, refreshExp));
        return TokenResponse.bearer(accessToken, refreshToken);
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

    public TokenResponse refreshTokens(String refreshToken) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);

        String jti = requiredClaim(decoded, "jti");
        String email = requiredClaim(decoded, "email");
        UUID userId = parseUuid(decoded.getSubject(), "유효하지 않은 사용자 토큰입니다");
        UUID orgId = parseUuid(requiredClaim(decoded, "orgId"), "유효하지 않은 조직 토큰입니다");
        String role = requiredClaim(decoded, "role");

        RefreshToken storedToken = refreshTokenRepository.findByTokenJti(jti)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID, "유효하지 않은 refresh 토큰입니다"));

        if (!storedToken.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰 사용자 정보가 일치하지 않습니다");
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "refresh 토큰이 만료되었습니다");
        }

        refreshTokenRepository.delete(storedToken);
        return issueTokens(userId, email, orgId, role);
    }

    public void logout(String refreshToken) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);
        String jti = requiredClaim(decoded, "jti");
        refreshTokenRepository.deleteByTokenJti(jti);
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
}
