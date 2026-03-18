package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import java.util.ArrayList;
import java.util.List;

public class TestAuthEmailPort implements AuthEmailPort {

    private final List<String> invitationEmails = new ArrayList<>();
    private final List<String> verificationEmails = new ArrayList<>();

    @Override
    public void sendVerificationCode(String email, String code) {
        verificationEmails.add(email + ":" + code);
    }

    @Override
    public void sendInvitation(String email, String orgName, String inviterName, String inviteUrl) {
        invitationEmails.add(email + ":" + inviteUrl);
    }

    public List<String> invitationEmails() {
        return List.copyOf(invitationEmails);
    }

    public List<String> verificationEmails() {
        return List.copyOf(verificationEmails);
    }
}
