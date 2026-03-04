package com.fabbitinc.server.domain.organization.model;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class WorkspaceSlugPolicy {
    private WorkspaceSlugPolicy() {
    }

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,48}[a-z0-9])?$");

    private static final Set<String> RESERVED_SLUGS = Set.copyOf(List.of(
            "www", "www1", "www2", "web", "site", "api", "app", "cdn", "static", "assets",
            "media", "mail", "smtp", "imap", "pop", "mx", "ftp", "sftp", "ssh", "ns1",
            "ns2", "ns3", "ns4", "dns", "vpn", "proxy", "gateway", "dev", "staging", "test",
            "qa", "uat", "sandbox", "prod", "production", "preview", "canary", "local", "localhost",
            "admin", "dashboard", "console", "panel", "auth", "login", "signup", "register", "sso", "oauth",
            "billing", "payment", "checkout", "help", "support", "docs", "wiki", "faq", "blog", "news",
            "press", "status", "health", "monitor", "metrics", "grafana", "fabbit", "fabbitinc", "fabbitapp",
            "abuse", "spam", "phishing", "security", "postmaster", "webmaster", "hostmaster", "noreply", "no-reply", "mailer-daemon",
            "root", "sysadmin", "administrator", "internal", "intranet", "extranet", "download", "downloads", "update", "updates"
    ));

    public static String validateFormat(String slug) {
        if (slug == null || slug.isBlank()) {
            return "슬러그는 최소 3자 이상이어야 합니다";
        }
        if (slug.length() < 3) {
            return "슬러그는 최소 3자 이상이어야 합니다";
        }
        if (slug.length() > 50) {
            return "슬러그는 최대 50자까지 가능합니다";
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            return "소문자 영문, 숫자, 하이픈(-)만 사용 가능하며, 하이픈으로 시작/끝할 수 없습니다";
        }
        if (RESERVED_SLUGS.contains(slug)) {
            return "사용할 수 없는 워크스페이스 주소입니다";
        }
        return null;
    }
}
