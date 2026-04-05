package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartResult;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreatePartUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public CreatePartResult execute(CreatePartCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        PartRevision draft = partService.createPart(new CreatePartInput(
                command.partNumber(),
                command.categoryId(),
                command.itemType(),
                command.name(),
                command.material(),
                command.unit(),
                command.description(),
                command.lifecycleState(),
                command.leadTimeDays(),
                command.extendedProperties(),
                command.reason()
        ), auth.userId());
        return new CreatePartResult(draft.getPartId(), draft.getId());
    }
}
