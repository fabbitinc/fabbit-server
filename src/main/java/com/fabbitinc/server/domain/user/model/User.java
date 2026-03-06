package com.fabbitinc.server.domain.user.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
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

    public static final String CODE_USER_EMAIL_REQUIRED = "USER_EMAIL_REQUIRED";
    public static final String CODE_USER_EMAIL_TOO_LONG = "USER_EMAIL_TOO_LONG";
    public static final String CODE_USER_PASSWORD_REQUIRED = "USER_PASSWORD_REQUIRED";
    public static final String CODE_USER_PASSWORD_TOO_LONG = "USER_PASSWORD_TOO_LONG";
    public static final String CODE_USER_FULL_NAME_REQUIRED = "USER_FULL_NAME_REQUIRED";
    public static final String CODE_USER_FULL_NAME_TOO_LONG = "USER_FULL_NAME_TOO_LONG";
    public static final String CODE_USER_PHONE_TOO_LONG = "USER_PHONE_TOO_LONG";
    public static final String CODE_USER_PROFILE_IMAGE_REQUIRED = "USER_PROFILE_IMAGE_REQUIRED";
    public static final String CODE_USER_PROFILE_IMAGE_TOO_LONG = "USER_PROFILE_IMAGE_TOO_LONG";
    public static final String CODE_USER_ALREADY_ACTIVE = "USER_ALREADY_ACTIVE";
    public static final String CODE_USER_ALREADY_INACTIVE = "USER_ALREADY_INACTIVE";

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PASSWORD_LENGTH = 255;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_PROFILE_IMAGE_LENGTH = 1000;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

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

    private User(String email, String hashedPassword, String fullName) {
        super(UuidV7Generator.next());
        this.email = requireEmail(email);
        this.hashedPassword = requireHashedPassword(hashedPassword);
        this.fullName = requireFullName(fullName);
        this.phone = null;
        this.profileImageFileKey = null;
        this.active = true;
    }

    public static User create(String email, String hashedPassword, String fullName) {
        return new User(email, hashedPassword, fullName);
    }

    public void changeProfile(String fullName, String phone) {
        if (fullName != null) {
            this.fullName = requireFullName(fullName);
        }
        if (phone != null) {
            this.phone = normalizePhone(phone);
        }
    }

    public void changePassword(String hashedPassword) {
        this.hashedPassword = requireHashedPassword(hashedPassword);
    }

    public void changeProfileImage(String profileImageFileKey) {
        this.profileImageFileKey = requireProfileImageFileKey(profileImageFileKey);
    }

    public void removeProfileImage() {
        this.profileImageFileKey = null;
    }

    public void activate() {
        if (active) {
            throw new DomainException(CODE_USER_ALREADY_ACTIVE, "이미 활성화된 사용자입니다");
        }
        this.active = true;
    }

    public void deactivate() {
        if (!active) {
            throw new DomainException(CODE_USER_ALREADY_INACTIVE, "이미 비활성화된 사용자입니다");
        }
        this.active = false;
    }

    private String requireEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_USER_EMAIL_REQUIRED, "이메일은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            throw new DomainException(CODE_USER_EMAIL_TOO_LONG, "이메일은 255자 이하여야 합니다");
        }
        return trimmed;
    }

    private String requireHashedPassword(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_USER_PASSWORD_REQUIRED, "비밀번호 해시는 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainException(CODE_USER_PASSWORD_TOO_LONG, "비밀번호 해시는 255자 이하여야 합니다");
        }
        return trimmed;
    }

    private String requireFullName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_USER_FULL_NAME_REQUIRED, "이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_FULL_NAME_LENGTH) {
            throw new DomainException(CODE_USER_FULL_NAME_TOO_LONG, "이름은 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_PHONE_LENGTH) {
            throw new DomainException(CODE_USER_PHONE_TOO_LONG, "전화번호는 20자 이하여야 합니다");
        }
        return trimmed;
    }

    private String requireProfileImageFileKey(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_USER_PROFILE_IMAGE_REQUIRED, "프로필 이미지 파일 키는 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_PROFILE_IMAGE_LENGTH) {
            throw new DomainException(CODE_USER_PROFILE_IMAGE_TOO_LONG, "프로필 이미지 파일 키는 1000자 이하여야 합니다");
        }
        return trimmed;
    }
}
