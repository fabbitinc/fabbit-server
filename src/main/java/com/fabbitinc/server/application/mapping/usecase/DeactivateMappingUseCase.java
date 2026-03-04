package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.mapping.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeactivateMappingUseCase {

    private final MappingService mappingService;

    @Transactional
    public void execute(UUID mappingId) {
        mappingService.deactivateMapping(mappingId);
    }
}
