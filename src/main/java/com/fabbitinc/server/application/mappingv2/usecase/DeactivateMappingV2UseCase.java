package com.fabbitinc.server.application.mappingv2.usecase;

import com.fabbitinc.server.application.mappingv2.service.MappingV2Service;
import com.fabbitinc.server.application.mappingv2.usecase.command.DeactivateMappingV2Command;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeactivateMappingV2UseCase {

    private final MappingV2Service mappingV2Service;

    public void execute(DeactivateMappingV2Command command) {
        mappingV2Service.deactivateMapping(command.mappingId());
    }
}
