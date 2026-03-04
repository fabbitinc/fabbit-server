package com.fabbitinc.server.domain.user.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AbstractAuditableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public User(String email, String hashedPassword, String fullName) {
        super(UuidV7Generator.next());
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.active = true;
    }
}
