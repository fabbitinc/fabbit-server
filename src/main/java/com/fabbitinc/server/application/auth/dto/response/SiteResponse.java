package com.fabbitinc.server.application.auth.dto.response;

public record SiteResponse(
        String slug,
        String name,
        String profileImageUrl
) {
}
