package com.fabbitinc.server.application.drawing.port;

import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.file.Path;

public interface Cad3dPreviewRenderPort {

    GeneratedBinary renderPreview(Path inputPath, String outputFileName) throws Exception;
}
