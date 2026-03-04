package com.fabbitinc.server.application.auth.support;

import com.fabbitinc.server.domain.organization.model.MembershipRole;

public interface CurrentAuthProvider {

    AuthContext getCurrentAuth();

    AuthContext getCurrentAuth(MembershipRole minimumRole);

    AuthContext getAdminAuth();
}
