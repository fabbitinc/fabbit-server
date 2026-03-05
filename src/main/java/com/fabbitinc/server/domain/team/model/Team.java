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
        this.name = name;
        this.description = description;
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
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
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
}
