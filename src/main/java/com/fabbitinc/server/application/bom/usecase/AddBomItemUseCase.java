package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.bom.service.input.AddBomItemInput;
import com.fabbitinc.server.application.bom.usecase.command.AddBomItemCommand;
import com.fabbitinc.server.application.bom.usecase.result.AddBomItemResult;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AddBomItemUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomService engineeringBomService;

    public AddBomItemResult execute(AddBomItemCommand command) {
        currentAuthProvider.getCurrentAuth();
        EngineeringBomItem item = engineeringBomService.addBomItem(new AddBomItemInput(
                command.partId(),
                command.revisionId(),
                command.childPartRevisionId(),
                command.lineNumber(),
                command.quantity(),
                command.extendedProperties()
        ));
        return new AddBomItemResult(command.partId(), command.revisionId(), item.getId());
    }
}
