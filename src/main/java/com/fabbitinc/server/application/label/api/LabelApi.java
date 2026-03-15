package com.fabbitinc.server.application.label.api;

import com.fabbitinc.server.application.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LabelApi {

    private final LabelService labelService;

    @Transactional
    public void ensureDefaultLabelsExist() {
        labelService.ensureDefaultLabelsExist();
    }
}
