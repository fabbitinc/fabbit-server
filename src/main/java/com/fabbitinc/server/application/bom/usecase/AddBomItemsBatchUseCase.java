package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.bom.service.input.AddBomItemsBatchInput;
import com.fabbitinc.server.application.bom.usecase.command.AddBomItemsBatchCommand;
import com.fabbitinc.server.application.bom.usecase.result.AddBomItemsBatchResult;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AddBomItemsBatchUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomService engineeringBomService;

    public AddBomItemsBatchResult execute(AddBomItemsBatchCommand command) {
        currentAuthProvider.getCurrentAuth();
        List<EngineeringBomItem> items = engineeringBomService.addBomItemsBatch(new AddBomItemsBatchInput(
                command.partId(),
                command.revisionId(),
                command.items().stream()
                        .map(item -> new AddBomItemsBatchInput.Item(
                                item.childPartRevisionId(),
                                item.lineNumber(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList()
        ));
        return new AddBomItemsBatchResult(
                command.partId(),
                command.revisionId(),
                items.stream().map(EngineeringBomItem::getId).toList()
        );
    }
}
