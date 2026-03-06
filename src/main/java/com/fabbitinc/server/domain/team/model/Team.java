package com.fabbitinc.server.domain.team.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "teams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_teams_name", columnNames = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends AbstractAuditableEntity {

    public static final String CODE_TEAM_CREATED_BY_REQUIRED = "TEAM_CREATED_BY_REQUIRED";
    public static final String CODE_TEAM_NAME_REQUIRED = "TEAM_NAME_REQUIRED";
    public static final String CODE_TEAM_NAME_TOO_LONG = "TEAM_NAME_TOO_LONG";

    private static final int MAX_NAME_LENGTH = 100;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<TeamMember> members = new ArrayList<>();

    public Team(String name, String description, UUID createdBy) {
        super(UuidV7Generator.next());
        this.name = validateName(name);
        this.description = normalizeDescription(description);
        this.createdBy = requireCreatedBy(createdBy);
    }

    public static Team create(String name, String description, UUID createdBy) {
        return new Team(name, description, createdBy);
    }

    public static Team create(String name, String description, User creator) {
        if (creator == null) {
            throw new DomainException(CODE_TEAM_CREATED_BY_REQUIRED, "생성자 ID는 필수입니다");
        }
        Team team = new Team(name, description, creator.getId());
        team.creator = creator;
        return team;
    }

    public void changeName(String name) {
        this.name = validateName(name);
    }

    public void changeDescription(String description) {
        this.description = normalizeDescription(description);
    }

    public List<TeamMember> getMembers() {
        return List.copyOf(members);
    }

    private UUID requireCreatedBy(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_TEAM_CREATED_BY_REQUIRED, "생성자 ID는 필수입니다");
        }
        return value;
    }

    private String validateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainException(CODE_TEAM_NAME_REQUIRED, "팀 이름은 필수입니다");
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_TEAM_NAME_TOO_LONG, "팀 이름은 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeDescription(String rawDescription) {
        if (rawDescription == null) {
            return null;
        }
        String trimmed = rawDescription.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
