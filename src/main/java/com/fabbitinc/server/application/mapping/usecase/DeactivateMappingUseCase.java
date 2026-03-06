package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.usecase.command.DeactivateMappingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeactivateMappingUseCase {

    private final MappingService mappingService;

    public void execute(DeactivateMappingCommand command) {
        mappingService.deactivateMapping(command.mappingId());
    }
}
