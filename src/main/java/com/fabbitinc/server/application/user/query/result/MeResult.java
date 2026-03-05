package com.fabbitinc.server.application.user.query.result;

import java.util.List;

public record MeResult(
        QueryUserResult user,
        List<UserMembershipResult> memberships
) {
}
