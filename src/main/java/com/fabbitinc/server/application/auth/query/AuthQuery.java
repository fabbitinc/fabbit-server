package com.fabbitinc.server.application.auth.query;

import com.fabbitinc.server.application.auth.dto.response.CheckSlugResponse;
import com.fabbitinc.server.application.auth.dto.response.CheckEmailResponse;
import com.fabbitinc.server.application.auth.dto.response.PlanResponse;
import com.fabbitinc.server.application.auth.dto.response.SiteResponse;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.OrganizationPlans;
import com.fabbitinc.server.domain.organization.model.PlanLimits;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.model.WorkspaceSlugPolicy;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthQuery {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans() {
        return OrganizationPlans.limits().entrySet().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckSlugResponse checkSlug(String slug) {
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        String error = WorkspaceSlugPolicy.validateFormat(normalizedSlug);
        if (error != null) {
            return new CheckSlugResponse(false, error, null);
        }

        if (organizationRepository.existsBySlug(normalizedSlug)) {
            String suggestion = normalizedSlug + "-" + UUID.randomUUID().toString().substring(0, 4);
            return new CheckSlugResponse(false, "이미 사용 중인 워크스페이스 주소입니다", suggestion);
        }
        return CheckSlugResponse.asAvailable();
    }

    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        boolean exists = userRepository.existsByEmail(normalizedEmail);
        return new CheckEmailResponse(
                !exists,
                exists ? "이미 가입된 이메일입니다" : null
        );
    }

    @Transactional(readOnly = true)
    public SiteResponse getSite(String origin) {
        String slug = extractOriginSlug(origin, appProperties.baseDomain());
        if (slug == null || slug.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "워크스페이스를 통해 접근해주세요");
        }

        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "존재하지 않는 워크스페이스입니다"));

        return new SiteResponse(
                organization.getSlug(),
                organization.getName(),
                organization.getProfileImageFileKey()
        );
    }

    private PlanResponse toPlanResponse(Map.Entry<PlanType, PlanLimits> entry) {
        PlanType planType = entry.getKey();
        PlanLimits limits = entry.getValue();
        return new PlanResponse(
                planType.name(),
                limits.displayName(),
                limits.description(),
                limits.maxMembers(),
                limits.storageGb(),
                limits.aiCredits(),
                limits.priceMonthly()
        );
    }

    private String extractOriginSlug(String origin, String baseDomain) {
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

        if (host.equals(baseDomain) || host.equals("www." + baseDomain)) {
            return null;
        }
        String suffix = "." + baseDomain;
        if (host.endsWith(suffix)) {
            return host.substring(0, host.length() - suffix.length());
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
