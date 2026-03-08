package com.fabbitinc.server.application.drawing.port;

import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.file.Path;

public interface Cad2dToPdfPort {

    GeneratedBinary convert(Path inputPath, String outputFileName) throws Exception;
}
