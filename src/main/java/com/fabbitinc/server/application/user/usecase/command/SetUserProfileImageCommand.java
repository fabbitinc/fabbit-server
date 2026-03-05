package com.fabbitinc.server.application.user.usecase.command;

import java.util.UUID;

public record SetUserProfileImageCommand(
        UUID fileId
) {
}
