package com.fabbitinc.server.application.auth.support;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.JwtProperties;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthTokenParser {

    private final JwtProperties jwtProperties;

    public AuthContext requireAuth(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        DecodedJWT decoded = verify(token, "access 토큰이 만료되었습니다", "유효하지 않은 access 토큰입니다");

        String tokenType = requiredClaim(decoded, "type");
        if (!"ACCESS".equals(tokenType)) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "access 토큰이 아닙니다");
        }

        UUID userId = parseUuid(decoded.getSubject(), "유효하지 않은 사용자 토큰입니다");
        UUID orgId = parseUuid(requiredClaim(decoded, "orgId"), "유효하지 않은 조직 토큰입니다");
        MembershipRole role;
        try {
            role = MembershipRole.from(requiredClaim(decoded, "role"));
        } catch (Exception ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "유효하지 않은 권한 토큰입니다");
        }

        return new AuthContext(userId, requiredClaim(decoded, "email"), orgId, role);
    }

    public Optional<UUID> resolveOrgIdForTenant(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(7);
        DecodedJWT decoded;
        try {
            decoded = JWT.require(algorithm())
                    .withIssuer(jwtProperties.issuer())
                    .build()
                    .verify(token);
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }

        if (!"ACCESS".equals(decoded.getClaim("type").asString())) {
            return Optional.empty();
        }

        String rawOrgId = decoded.getClaim("orgId").asString();
        if (rawOrgId == null || rawOrgId.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(rawOrgId));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public AuthContext requireRole(String authorizationHeader, MembershipRole minimumRole) {
        AuthContext auth = requireAuth(authorizationHeader);
        if (!auth.role().atLeast(minimumRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, minimumRole.name() + " 이상 권한이 필요합니다");
        }
        return auth;
    }

    public AuthContext requireAdmin(String authorizationHeader) {
        return requireRole(authorizationHeader, MembershipRole.ADMIN);
    }

    public CreateOrgContext requireCreateOrgToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        DecodedJWT decoded = verify(token, "조직 생성 토큰이 만료되었습니다", "유효하지 않은 조직 생성 토큰입니다");

        String tokenType = requiredClaim(decoded, "type");
        String scope = decoded.getClaim("scope").asString();
        if (!"SCOPED".equals(tokenType) || !"create_org".equals(scope)) {
            throw new AppException(ErrorCode.FORBIDDEN, "조직 생성 권한이 없는 토큰입니다");
        }

        UUID userId = parseUuid(decoded.getSubject(), "유효하지 않은 사용자 토큰입니다");
        return new CreateOrgContext(userId, requiredClaim(decoded, "email"));
    }

    private DecodedJWT verify(String token, String expiredMessage, String invalidMessage) {
        try {
            return JWT.require(algorithm())
                    .withIssuer(jwtProperties.issuer())
                    .build()
                    .verify(token);
        } catch (TokenExpiredException ex) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, expiredMessage);
        } catch (JWTVerificationException ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, invalidMessage);
        }
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(jwtProperties.secretKey());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "인증이 필요합니다");
        }
        return authorizationHeader.substring(7);
    }

    private String requiredClaim(DecodedJWT decodedJWT, String claimName) {
        String value = decodedJWT.getClaim(claimName).asString();
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "토큰 claim(" + claimName + ")이 비어 있습니다");
        }
        return value;
    }

    private UUID parseUuid(String rawValue, String message) {
        try {
            return UUID.fromString(rawValue);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.TOKEN_INVALID, message);
        }
    }
}
