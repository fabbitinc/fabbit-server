package com.fabbitinc.server.application.drawing.port;

import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.file.Path;

public interface Cad3dToGlbPort {

    GeneratedBinary convertToGlb(Path inputPath, String outputFileName) throws Exception;
}
