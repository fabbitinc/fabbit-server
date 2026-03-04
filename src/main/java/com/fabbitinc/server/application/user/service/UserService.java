package com.fabbitinc.server.application.user.service;

import com.fabbitinc.server.application.auth.service.PasswordService;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public User createUser(String email, String password, String fullName) {
        User user = new User(normalizeEmail(email), passwordService.hash(password), fullName);
        return userRepository.save(user);
    }

    public UserWithNewFlag findOrCreateForInvitation(String email, String password, String fullName) {
        Optional<User> existingUser = userRepository.findByEmail(normalizeEmail(email));
        if (existingUser.isPresent()) {
            return new UserWithNewFlag(existingUser.get(), false);
        }

        if (password == null || password.isBlank() || fullName == null || fullName.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "신규 가입 시 비밀번호와 이름이 필요합니다");
        }

        User user = createUser(email, password, fullName);
        return new UserWithNewFlag(user, true);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    public void updateProfile(UUID userId, String fullName, String phone) {
        User user = getUserOrThrow(userId);
        user.updateProfile(fullName, phone);
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUserOrThrow(userId);

        if (!passwordService.matches(currentPassword, user.getHashedPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 올바르지 않습니다");
        }

        user.changePassword(passwordService.hash(newPassword));
    }

    public void setProfileImage(UUID userId, File file) {
        User user = getUserOrThrow(userId);
        user.setProfileImage(file.getFileKey());
        file.assignOwner("user", userId);
    }

    public void deleteProfileImage(UUID userId) {
        User user = getUserOrThrow(userId);
        user.removeProfileImage();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record UserWithNewFlag(User user, boolean isNewUser) {
    }
}
