package com.fabbitinc.server.application.user.api;

import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.user.model.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserApi {

    private final UserService userService;

    public List<User> getUsersByIdsOrdered(List<UUID> userIds) {
        return userService.getUsersByIdsOrdered(userIds);
    }

    public User getUserOrNull(UUID userId) {
        return userService.getUserOrNull(userId);
    }
}
