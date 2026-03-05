package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.LinkProjectPartsCommand;
import com.fabbitinc.server.application.project.usecase.result.LinkProjectPartsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class LinkProjectPartsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public LinkProjectPartsResult execute(LinkProjectPartsCommand command) {
        currentAuthProvider.getCurrentAuth();
        int linkedCount = projectService.linkParts(command.projectId(), command.partIds());
        return new LinkProjectPartsResult(linkedCount);
    }
}
