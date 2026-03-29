package com.fabbitinc.server.application.bom.usecase.result;

import com.fabbitinc.server.application.bom.usecase.command.CommitBomImportCommand.BomImportMode;
import java.util.List;
import java.util.UUID;

public record CommitBomImportResult(
        List<UUID> createdBomItemIds,
        Summary summary
) {

    public record Summary(
            int totalCreated,
            BomImportMode mode
    ) {
    }
}
