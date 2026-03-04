package com.fabbitinc.server.application.auth.port;

public interface AuthEmailPort {

    void sendVerificationCode(String email, String code);

    void sendInvitation(String email, String orgName, String inviterName, String inviteUrl);
}
