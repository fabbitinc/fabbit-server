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
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AbstractAuditableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "email2", nullable = false, length = 255)
    private String email2;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image_file_key", length = 1000)
    private String profileImageFileKey;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public User(String email, String hashedPassword, String fullName) {
        super(UuidV7Generator.next());
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.active = true;
    }

    public void updateProfile(String fullName, String phone) {
        if (fullName != null) {
            this.fullName = fullName;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void changePassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setProfileImage(String profileImageFileKey) {
        this.profileImageFileKey = profileImageFileKey;
    }

    public void removeProfileImage() {
        this.profileImageFileKey = null;
    }
}
