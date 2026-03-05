package com.fabbitinc.server.application.member.query.result;

import java.util.List;

public record MemberLookupResult(
        List<MemberLookupItemResult> items
) {
}
