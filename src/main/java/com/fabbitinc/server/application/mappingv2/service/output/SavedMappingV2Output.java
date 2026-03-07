package com.fabbitinc.server.application.mappingv2.service.output;

import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;

public record SavedMappingV2Output(
        MappingV2Record record,
        MappingV2Revision revision
) {
}
