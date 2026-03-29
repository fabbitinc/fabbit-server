package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.bom.service.input.DeleteBomItemInput;
import com.fabbitinc.server.application.bom.usecase.command.DeleteBomItemCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeleteBomItemUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomService engineeringBomService;

    public void execute(DeleteBomItemCommand command) {
        currentAuthProvider.getCurrentAuth();
        engineeringBomService.deleteBomItem(new DeleteBomItemInput(
                command.partId(),
                command.revisionId(),
                command.bomItemId()
        ));
    }
}
