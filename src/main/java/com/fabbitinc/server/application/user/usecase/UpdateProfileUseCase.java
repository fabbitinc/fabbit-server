package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.user.dto.request.UpdateProfileRequest;
import com.fabbitinc.server.application.user.dto.response.UpdateProfileResponse;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final AuthTokenParser authTokenParser;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public UpdateProfileResponse execute(String authorizationHeader, UpdateProfileRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        userService.updateProfile(auth.userId(), request.fullName(), request.phone());
        User user = userService.getUserOrThrow(auth.userId());

        return new UpdateProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.getUpdatedAt()
        );
    }
}
