package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartOwnerCommand;
import com.fabbitinc.server.application.part.usecase.result.UpdatePartOwnerResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartOwnerUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRouteService partRevisionRouteService;
    private final PartService partService;

    public UpdatePartOwnerResult execute(UpdatePartOwnerCommand command) {
        currentAuthProvider.getCurrentAuth();
        var partRevisionId = partRevisionRouteService.getRequiredRevisionId(command.partNumber(), command.revisionCode());
        return new UpdatePartOwnerResult(
                partService.updateOwner(
                        partRevisionId,
                        command.ownerId(),
                        command.ownerIdSet(),
                        command.ownerTeamId(),
                        command.ownerTeamIdSet()
                ).getId()
        );
    }
}
