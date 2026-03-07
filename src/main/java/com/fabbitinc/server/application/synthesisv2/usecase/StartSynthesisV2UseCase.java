package com.fabbitinc.server.application.synthesisv2.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.synthesisv2.service.SynthesisV2Service;
import com.fabbitinc.server.application.synthesisv2.service.input.StartSynthesisV2Input;
import com.fabbitinc.server.application.synthesisv2.service.input.SynthesisV2UploadInput;
import com.fabbitinc.server.application.synthesisv2.service.output.SynthesisV2BatchStartOutput;
import com.fabbitinc.server.application.synthesisv2.usecase.command.StartSynthesisV2Command;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartSynthesisV2FailureResult;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartedSynthesisV2BatchResult;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartedSynthesisV2JobResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class StartSynthesisV2UseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SynthesisV2Service synthesisV2Service;

    public StartedSynthesisV2BatchResult execute(StartSynthesisV2Command command) {
        currentAuthProvider.getCurrentAuth();
        SynthesisV2BatchStartOutput output = synthesisV2Service.startSynthesis(
                new StartSynthesisV2Input(
                        command.mappingId(),
                        command.projectId(),
                        command.overwrite(),
                        command.uploads().stream()
                                .map(item -> new SynthesisV2UploadInput(item.fileId(), item.rootContext()))
                                .toList()
                )
        );

        return new StartedSynthesisV2BatchResult(
                output.batchId(),
                output.requestedCount(),
                output.acceptedCount(),
                output.items().stream()
                        .map(item -> new StartedSynthesisV2JobResult(
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
                        .map(item -> new StartSynthesisV2FailureResult(item.fileId(), item.reason()))
                        .toList()
        );
    }
}
