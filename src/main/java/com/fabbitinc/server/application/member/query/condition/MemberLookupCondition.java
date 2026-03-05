package com.fabbitinc.server.application.member.query.condition;

public record MemberLookupCondition(
        String search,
        int limit
) {
}
