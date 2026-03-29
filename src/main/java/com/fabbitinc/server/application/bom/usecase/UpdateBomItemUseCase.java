package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.bom.service.input.UpdateBomItemInput;
import com.fabbitinc.server.application.bom.usecase.command.UpdateBomItemCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateBomItemUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomService engineeringBomService;

    public void execute(UpdateBomItemCommand command) {
        currentAuthProvider.getCurrentAuth();
        engineeringBomService.updateBomItem(new UpdateBomItemInput(
                command.partId(),
                command.revisionId(),
                command.bomItemId(),
                command.childPartRevisionId(),
                command.childPartRevisionIdSet(),
                command.lineNumber(),
                command.lineNumberSet(),
                command.quantity(),
                command.quantitySet(),
                command.extendedProperties(),
                command.extendedPropertiesSet()
        ));
    }
}
