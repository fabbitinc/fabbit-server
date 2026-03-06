package com.fabbitinc.server.application.activation.usecase;

import com.fabbitinc.server.application.activation.service.ActivationService;
import com.fabbitinc.server.application.activation.service.output.GraphQueryOutput;
import com.fabbitinc.server.application.activation.usecase.command.QueryGraphCommand;
import com.fabbitinc.server.application.activation.usecase.result.QueryGraphItemResult;
import com.fabbitinc.server.application.activation.usecase.result.QueryGraphResult;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryGraphUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ActivationService activationService;

    public QueryGraphResult execute(QueryGraphCommand command) {
        currentAuthProvider.getCurrentAuth();
        GraphQueryOutput output = activationService.queryGraph(command.question());
        return new QueryGraphResult(
                output.results().stream()
                        .map(item -> new QueryGraphItemResult(
                                item.type(),
                                item.key(),
                                item.label(),
                                item.description(),
                                item.value()
                        ))
                        .toList(),
                output.answer()
        );
    }
}
