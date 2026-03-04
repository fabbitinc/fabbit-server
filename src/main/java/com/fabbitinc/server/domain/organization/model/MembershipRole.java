package com.fabbitinc.server.domain.organization.model;

public enum MembershipRole {
    MEMBER(0),
    ADMIN(1),
    OWNER(2);

    private final int level;

    MembershipRole(int level) {
        this.level = level;
    }

    public boolean atLeast(MembershipRole minimumRole) {
        return this.level >= minimumRole.level;
    }

    public boolean canManage(MembershipRole targetRole) {
        if (this == OWNER) {
            return true;
        }
        return this.level > targetRole.level;
    }

    public static MembershipRole from(String rawRole) {
        return MembershipRole.valueOf(rawRole.trim().toUpperCase());
    }
}
