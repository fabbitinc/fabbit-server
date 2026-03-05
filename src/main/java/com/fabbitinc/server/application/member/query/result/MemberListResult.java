package com.fabbitinc.server.application.member.query.result;

import java.util.List;

public record MemberListResult(
        List<MemberSummaryResult> items,
        int maxMembers
) {
}
