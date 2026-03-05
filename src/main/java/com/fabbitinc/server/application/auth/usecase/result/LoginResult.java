package com.fabbitinc.server.application.auth.usecase.result;

public record LoginResult(
        AuthUserResult user,
        AuthTokenResult tokens,
        String scopedAccessToken,
        boolean scoped
) {
    public static LoginResult scoped(AuthUserResult user, String scopedAccessToken) {
        return new LoginResult(user, null, scopedAccessToken, true);
    }

    public static LoginResult organization(AuthUserResult user, AuthTokenResult tokens) {
        return new LoginResult(user, tokens, null, false);
    }
}
