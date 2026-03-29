package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.input.CreatePartDraftInput;
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreatePartDraftUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionService partRevisionService;
    private final EngineeringBomService engineeringBomService;

    public CreatePartDraftResult execute(CreatePartDraftCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        PartRevision draft = partRevisionService.createDraft(new CreatePartDraftInput(
                command.partId(),
                command.baseRevisionId(),
                command.reason()
        ), auth.userId());
        engineeringBomService.copyBomItems(command.baseRevisionId(), draft.getId());
        return new CreatePartDraftResult(draft.getPartId(), draft.getId());
    }
}
