package com.fabbitinc.server.application.mapping.service.output;

import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;

public record SavedMappingOutput(
        MappingRecord record,
        MappingRevision revision
) {
}
