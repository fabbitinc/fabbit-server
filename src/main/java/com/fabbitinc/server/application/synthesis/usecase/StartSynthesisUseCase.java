package com.fabbitinc.server.application.synthesis.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.synthesis.service.SynthesisService;
import com.fabbitinc.server.application.synthesis.service.input.StartSynthesisInput;
import com.fabbitinc.server.application.synthesis.service.input.SynthesisUploadInput;
import com.fabbitinc.server.application.synthesis.service.output.SynthesisBatchStartOutput;
import com.fabbitinc.server.application.synthesis.usecase.command.StartSynthesisCommand;
import com.fabbitinc.server.application.synthesis.usecase.result.StartSynthesisFailureResult;
import com.fabbitinc.server.application.synthesis.usecase.result.StartedSynthesisBatchResult;
import com.fabbitinc.server.application.synthesis.usecase.result.StartedSynthesisJobResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class StartSynthesisUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SynthesisService synthesisV2Service;

    public StartedSynthesisBatchResult execute(StartSynthesisCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        SynthesisBatchStartOutput output = synthesisV2Service.startSynthesis(
                new StartSynthesisInput(
                        command.mappingId(),
                        command.projectId(),
                        auth.userId(),
                        command.overwrite(),
                        command.uploads().stream()
                                .map(item -> new SynthesisUploadInput(item.fileId(), item.rootContext()))
                                .toList()
                )
        );

        return new StartedSynthesisBatchResult(
                output.batchId(),
                output.requestedCount(),
                output.acceptedCount(),
                output.items().stream()
                        .map(item -> new StartedSynthesisJobResult(
                                item.id(),
                                item.mappingId(),
                                item.fileId(),
                                item.status(),
                                item.totalRows(),
                                item.processedRows(),
                                item.nodesCreated(),
                                item.relationshipsCreated(),
                                item.errors(),
                                item.startedAt(),
                                item.completedAt(),
                                item.createdAt()
                        ))
                        .toList(),
                output.failed().stream()
                        .map(item -> new StartSynthesisFailureResult(item.fileId(), item.reason()))
                        .toList()
        );
    }
}
