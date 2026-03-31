package com.fabbitinc.server.application.migration.usecase.command;

import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import java.util.List;

public record StartInventorMigrationCommand(
        String projectName,
        String ipjPath,
        String inventorVersion,
        List<InventorManifestFile> files
) {
}
