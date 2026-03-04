package com.fabbitinc.server.application.activation.dto.response;

import java.util.List;

public record StartersResponse(
        List<StarterQuestionResponse> starters
) {
}
