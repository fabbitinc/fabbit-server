package com.fabbitinc.server.domain.team.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fabbitinc.server.domain.user.model.User;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "team_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_team_members_team_id_user_id",
                        columnNames = {"team_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "ix_team_members_team_id", columnList = "team_id"),
                @Index(name = "ix_team_members_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember extends AbstractCreatedEntity {

    public static final String CODE_TEAM_MEMBER_TEAM_REQUIRED = "TEAM_MEMBER_TEAM_REQUIRED";
    public static final String CODE_TEAM_MEMBER_USER_REQUIRED = "TEAM_MEMBER_USER_REQUIRED";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_members_team_id"))
    private Team team;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public TeamMember(Team team, UUID userId) {
        super(UuidV7Generator.next());
        this.team = requireTeam(team);
        this.userId = requireUserId(userId);
    }

    public static TeamMember assign(Team team, UUID userId) {
        return new TeamMember(team, userId);
    }

    public static TeamMember assign(Team team, User user) {
        if (user == null) {
            throw new DomainException(CODE_TEAM_MEMBER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        TeamMember member = new TeamMember(team, user.getId());
        member.user = user;
        return member;
    }

    public UUID getTeamId() {
        return team.getId();
    }

    private Team requireTeam(Team value) {
        if (value == null) {
            throw new DomainException(CODE_TEAM_MEMBER_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_TEAM_MEMBER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }
}
