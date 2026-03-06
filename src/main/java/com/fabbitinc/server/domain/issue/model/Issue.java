package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "issues",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issues_number", columnNames = "number")
        },
        indexes = {
                @Index(name = "ix_issues_type_state", columnList = "type,state"),
                @Index(name = "ix_issues_created_by", columnList = "created_by")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("ISSUE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends AbstractAuditableEntity {

    public static final String CODE_ISSUE_ACTOR_REQUIRED = "ISSUE_ACTOR_REQUIRED";
    public static final String CODE_ISSUE_TITLE_REQUIRED = "ISSUE_TITLE_REQUIRED";
    public static final String CODE_ISSUE_TITLE_TOO_LONG = "ISSUE_TITLE_TOO_LONG";

    private static final int MAX_TITLE_LENGTH = 500;

    @Column(name = "number", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, insertable = false, updatable = false)
    private IssueType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private IssueState state;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdByUser;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User updatedByUser;

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueAssignee> assignees = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueTeamAssignee> teamAssignees = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssuePart> parts = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueLabel> labels = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<ChangeRequestIssue> linkedChangeRequests = new ArrayList<>();

    public Issue(int number, String title, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.number = number;
        this.title = requireTitle(title);
        this.body = body;
        this.state = IssueState.OPEN;
        UUID requiredActorId = requireActorId(actorId);
        this.createdBy = requiredActorId;
        this.updatedBy = requiredActorId;
    }

    public static Issue create(int number, String title, String body, UUID actorId) {
        return new Issue(number, title, body, actorId);
    }

    public static Issue create(int number, String title, String body, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        Issue issue = new Issue(number, title, body, actor.getId());
        issue.createdByUser = actor;
        issue.updatedByUser = actor;
        return issue;
    }

    public void updateTitle(String title, UUID actorId) {
        String requiredTitle = requireTitle(title);
        UUID requiredActorId = requireActorId(actorId);
        this.title = requiredTitle;
        this.updatedBy = requiredActorId;
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void updateTitle(String title, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.title = requireTitle(title);
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void updateBody(String body, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.body = body;
        this.updatedBy = requiredActorId;
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void updateBody(String body, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.body = body;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void close(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.state = IssueState.CLOSED;
        this.closedAt = now;
        this.updatedBy = requiredActorId;
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void close(Instant now, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.state = IssueState.CLOSED;
        this.closedAt = now;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void reopen(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.state = IssueState.OPEN;
        this.closedAt = null;
        this.updatedBy = requiredActorId;
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void reopen(User actor) {
        if (actor == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.state = IssueState.OPEN;
        this.closedAt = null;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    protected void markClosed(Instant now, UUID actorId) {
        close(now, actorId);
    }

    protected void markOpen(UUID actorId) {
        reopen(actorId);
    }

    public List<IssueAssignee> getAssignees() {
        return List.copyOf(assignees);
    }

    public List<IssueTeamAssignee> getTeamAssignees() {
        return List.copyOf(teamAssignees);
    }

    public List<IssuePart> getParts() {
        return List.copyOf(parts);
    }

    public List<IssueLabel> getLabels() {
        return List.copyOf(labels);
    }

    public List<IssueComment> getComments() {
        return List.copyOf(comments);
    }

    public List<ChangeRequestIssue> getLinkedChangeRequests() {
        return List.copyOf(linkedChangeRequests);
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return value;
    }

    private String requireTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ISSUE_TITLE_REQUIRED, "이슈 제목은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new DomainException(CODE_ISSUE_TITLE_TOO_LONG, "이슈 제목은 500자 이하여야 합니다");
        }
        return trimmed;
    }
}
