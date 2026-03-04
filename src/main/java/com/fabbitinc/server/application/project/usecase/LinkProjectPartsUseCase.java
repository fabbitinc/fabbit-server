package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.dto.response.LinkPartsResponse;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LinkProjectPartsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    @Transactional
    public LinkPartsResponse execute(UUID projectId, List<UUID> partIds) {
        currentAuthProvider.getCurrentAuth();
        int linkedCount = projectService.linkParts(projectId, partIds);
        return new LinkPartsResponse(linkedCount);
    }
}
