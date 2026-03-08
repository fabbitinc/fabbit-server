package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.drawing.port.Cad3dPreviewRenderPort;
import com.fabbitinc.server.application.drawing.port.Cad3dToGlbPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Mayo3dConverterAdapter implements Cad3dToGlbPort, Cad3dPreviewRenderPort {

    private static final long TIMEOUT_SECONDS = 300;

    private final DrawingConverterProperties drawingConverterProperties;

    @Override
    public GeneratedBinary convertToGlb(Path inputPath, String outputFileName) throws Exception {
        return convert(inputPath, outputFileName, "model/gltf-binary");
    }

    @Override
    public GeneratedBinary renderPreview(Path inputPath, String outputFileName) throws Exception {
        return convert(inputPath, outputFileName, "image/png");
    }

    private GeneratedBinary convert(Path inputPath, String outputFileName, String contentType) throws Exception {
        Path workDir = inputPath.getParent();
        if (workDir == null) {
            throw new IllegalArgumentException("3D 변환 작업 디렉터리를 확인할 수 없습니다");
        }

        Path outputPath = workDir.resolve(outputFileName);
        List<String> command = buildCommand(inputPath, outputPath, workDir);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("3d converter output: {}", output);

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("3D 변환 실행 시간이 초과되었습니다");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("3D 변환 실행에 실패했습니다: " + output);
        }
        if (!Files.exists(outputPath)) {
            throw new IllegalStateException("3D 변환 결과 파일이 생성되지 않았습니다: " + outputFileName);
        }

        return new GeneratedBinary(outputFileName, contentType, Files.readAllBytes(outputPath));
    }

    private List<String> buildCommand(Path inputPath, Path outputPath, Path workDir) {
        String binPath = normalizeNullable(drawingConverterProperties.threeDConverterBinPath());
        if (binPath != null) {
            return List.of(
                    binPath,
                    inputPath.toAbsolutePath().toString(),
                    "-e",
                    outputPath.toAbsolutePath().toString()
            );
        }
        return buildLocalCommand(inputPath, outputPath, workDir);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * @deprecated 로컬 개발 환경 전용 Docker fallback이다. 3D 변환 바이너리 경로가 준비되면 삭제한다.
     */
    @Deprecated(forRemoval = false)
    private List<String> buildLocalCommand(Path inputPath, Path outputPath, Path workDir) {
        final String containerWorkDir = "/data";

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-v");
        command.add(workDir.toAbsolutePath() + ":" + containerWorkDir);
        command.add("fabbit-3dconverter:latest");
        command.add(containerWorkDir + "/" + inputPath.getFileName().toString());
        command.add("-e");
        command.add(containerWorkDir + "/" + outputPath.getFileName().toString());
        return command;
    }
}
