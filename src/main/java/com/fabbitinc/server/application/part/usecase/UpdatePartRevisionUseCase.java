package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.input.UpdatePartRevisionInput;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartRevisionCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartRevisionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionService partRevisionService;

    public void execute(UpdatePartRevisionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionService.updateDraft(new UpdatePartRevisionInput(
                command.partId(),
                command.revisionId(),
                command.name(),
                command.nameSet(),
                command.material(),
                command.materialSet(),
                command.unit(),
                command.unitSet(),
                command.description(),
                command.descriptionSet(),
                command.category(),
                command.categorySet(),
                command.phantom(),
                command.phantomSet(),
                command.leadTimeDays(),
                command.leadTimeDaysSet(),
                command.extendedProperties(),
                command.extendedPropertiesSet()
        ), auth.userId());
    }
}
