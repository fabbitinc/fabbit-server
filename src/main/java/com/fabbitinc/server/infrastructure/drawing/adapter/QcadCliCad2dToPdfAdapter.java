package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.drawing.port.Cad2dToPdfPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class QcadCliCad2dToPdfAdapter implements Cad2dToPdfPort {

    private final DrawingConverterProperties drawingConverterProperties;

    @Override
    public GeneratedBinary convert(Path inputPath, String outputFileName) throws Exception {
        Path executable = Paths.get(drawingConverterProperties.qcadPath(), "dwg2pdf");
        if (!Files.exists(executable)) {
            throw new IllegalStateException("QCAD dwg2pdf 실행 파일을 찾을 수 없습니다: " + executable);
        }

        Path outputPath = inputPath.getParent().resolve(outputFileName);
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        if (!isMac()) {
            command.add("-platform");
            command.add("offscreen");
        }
        command.add("-f");
        command.add("-auto-fit");
        command.add("-auto-orientation");
        command.add("-o");
        command.add(outputPath.toString());
        command.add(inputPath.toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        try {
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("qcad output: {}", output);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("QCAD dwg2pdf 실행 시간이 초과되었습니다");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("QCAD dwg2pdf 실패: " + output);
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("QCAD dwg2pdf 실행이 중단되었습니다", ex);
        }
        if (!Files.exists(outputPath)) {
            throw new IllegalStateException("QCAD dwg2pdf 결과 PDF가 생성되지 않았습니다");
        }

        return new GeneratedBinary(outputFileName, "application/pdf", Files.readAllBytes(outputPath));
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
