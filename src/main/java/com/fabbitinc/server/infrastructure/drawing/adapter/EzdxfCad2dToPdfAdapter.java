package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.drawing.port.Cad2dToPdfPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EzdxfCad2dToPdfAdapter implements Cad2dToPdfPort {

    private final DrawingConverterProperties drawingConverterProperties;

    @Override
    public GeneratedBinary convert(Path inputPath, String outputFileName) throws Exception {
        requireDxf(inputPath);
        Path executable = Paths.get(requireEzdxfBinPath());
        if (!Files.exists(executable)) {
            throw new IllegalStateException("ezdxf 실행 파일을 찾을 수 없습니다: " + executable);
        }

        Path outputPath = requireWorkDir(inputPath).resolve(outputFileName);
        Process process = new ProcessBuilder(
                executable.toString(),
                "draw",
                "--backend",
                "matplotlib",
                "--background",
                "WHITE",
                "-f",
                "-o",
                outputPath.toAbsolutePath().toString(),
                inputPath.toAbsolutePath().toString()
        )
                .redirectErrorStream(true)
                .start();

        try {
            boolean finished = process.waitFor(drawingConverterProperties.commandTimeoutSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("ezdxf output: {}", output);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("ezdxf 실행 시간이 초과되었습니다");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("ezdxf draw 실행에 실패했습니다: " + output);
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ezdxf 실행이 중단되었습니다", ex);
        }
        if (!Files.exists(outputPath)) {
            throw new IllegalStateException("ezdxf 결과 PDF가 생성되지 않았습니다");
        }
        return new GeneratedBinary(outputFileName, "application/pdf", Files.readAllBytes(outputPath));
    }

    private Path requireWorkDir(Path inputPath) {
        Path workDir = inputPath.getParent();
        if (workDir == null) {
            throw new IllegalArgumentException("2D 변환 작업 디렉터리를 확인할 수 없습니다");
        }
        return workDir;
    }

    private void requireDxf(Path inputPath) {
        String fileName = inputPath.getFileName() == null ? "" : inputPath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".dxf")) {
            throw new IllegalStateException("ezdxf는 DXF render source만 지원합니다");
        }
    }

    private String requireEzdxfBinPath() {
        String path = drawingConverterProperties.ezdxfBinPath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("ezdxf bin path 설정이 필요합니다");
        }
        return path.trim();
    }
}
