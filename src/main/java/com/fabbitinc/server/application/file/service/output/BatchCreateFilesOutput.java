package com.fabbitinc.server.application.file.service.output;

import java.util.List;

public record BatchCreateFilesOutput(
        List<CreateFileOutput> items
) {
}
