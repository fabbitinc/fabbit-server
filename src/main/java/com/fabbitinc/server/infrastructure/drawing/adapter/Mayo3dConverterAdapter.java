package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.drawing.port.Cad3dPreviewRenderPort;
import com.fabbitinc.server.application.drawing.port.Cad3dToGlbPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Mayo3dConverterAdapter implements Cad3dToGlbPort, Cad3dPreviewRenderPort {

    private static final String PREVIEW_SETTINGS_RESOURCE_PATH = "drawing/mayo/settings.ini";

    private final DrawingConverterProperties drawingConverterProperties;

    @Override
    public GeneratedBinary convertToGlb(Path inputPath, String outputFileName) throws Exception {
        Path workDir = requireWorkDir(inputPath);
        Path outputPath = workDir.resolve(outputFileName);
        List<String> command = buildGlbCommand(inputPath, outputPath, workDir);
        return execute(
                inputPath,
                outputFileName,
                outputPath,
                "model/gltf-binary",
                "glb",
                null,
                command
        );
    }

    @Override
    public GeneratedBinary renderPreview(Path inputPath, String outputFileName) throws Exception {
        Path workDir = requireWorkDir(inputPath);
        Path outputPath = workDir.resolve(outputFileName);
        Path settingsPath = preparePreviewSettingsFile(workDir);
        List<String> command = buildPngCommand(inputPath, outputPath, workDir, settingsPath);
        return execute(
                inputPath,
                outputFileName,
                outputPath,
                "image/png",
                "preview_png",
                settingsPath,
                command
        );
    }

    private GeneratedBinary execute(
            Path inputPath,
            String outputFileName,
            Path outputPath,
            String contentType,
            String stage,
            Path settingsPath,
            List<String> command
    ) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        Thread outputReader = startOutputReader(process, outputBuffer);

        boolean finished;
        String output;
        try {
            finished = process.waitFor(drawingConverterProperties.commandTimeoutSeconds(), TimeUnit.SECONDS);
            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            output = outputBuffer.toString(StandardCharsets.UTF_8);
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("3D 변환 실행이 중단되었습니다: stage=" + stage, ex);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("3D 변환 실행 시간이 초과되었습니다: stage=" + stage);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("3D 변환 실행에 실패했습니다: stage=" + stage + ", output=" + output);
        }
        if (!Files.exists(outputPath)) {
            throw new IllegalStateException("3D 변환 결과 파일이 생성되지 않았습니다: stage=" + stage + ", file=" + outputFileName);
        }

        return new GeneratedBinary(outputFileName, contentType, Files.readAllBytes(outputPath));
    }

    private Path requireWorkDir(Path inputPath) {
        Path workDir = inputPath.getParent();
        if (workDir == null) {
            throw new IllegalArgumentException("3D 변환 작업 디렉터리를 확인할 수 없습니다");
        }
        return workDir;
    }

    private List<String> buildGlbCommand(Path inputPath, Path outputPath, Path workDir) {
        String binPath = normalizeNullable(drawingConverterProperties.threeDConverterBinPath());
        if (binPath == null) {
            return buildLocalGlbCommand(inputPath, outputPath, workDir);
        }

        return List.of(
                binPath,
                inputPath.toAbsolutePath().toString(),
                "-e",
                outputPath.toAbsolutePath().toString()
        );
    }

    private List<String> buildPngCommand(Path inputPath, Path outputPath, Path workDir, Path settingsPath) {
        String binPath = normalizeNullable(drawingConverterProperties.threeDConverterBinPath());
        if (binPath == null) {
            return buildLocalPngCommand(inputPath, outputPath, workDir, settingsPath);
        }

        return List.of(
                "xvfb-run",
                "--auto-servernum",
                binPath,
                inputPath.toAbsolutePath().toString(),
                "-u",
                settingsPath.toAbsolutePath().toString(),
                "-e",
                outputPath.toAbsolutePath().toString()
        );
    }

    private Path preparePreviewSettingsFile(Path workDir) throws IOException {
        Path targetPath = workDir.resolve("settings.ini");
        try (var inputStream = Mayo3dConverterAdapter.class.getClassLoader()
                .getResourceAsStream(PREVIEW_SETTINGS_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("3D preview settings.ini 리소스를 찾을 수 없습니다");
            }
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Thread startOutputReader(Process process, ByteArrayOutputStream outputBuffer) {
        Thread thread = new Thread(() -> {
            try (var inputStream = process.getInputStream()) {
                inputStream.transferTo(outputBuffer);
            } catch (java.io.IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }, "mayo-3d-output-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * @deprecated 로컬 개발 환경 전용 Docker fallback이다. 3D 변환 바이너리 경로가 준비되면 삭제한다.
     */
    @Deprecated(forRemoval = false)
    private List<String> buildLocalGlbCommand(Path inputPath, Path outputPath, Path workDir) {
        final String containerWorkDir = "/data";
        final String localThreeDConverterImage = "fabbit-3dconverter:latest";

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-v");
        command.add(workDir.toAbsolutePath() + ":" + containerWorkDir);
        command.add(localThreeDConverterImage);
        command.add(containerWorkDir + "/" + inputPath.getFileName());
        command.add("-e");
        command.add(containerWorkDir + "/" + outputPath.getFileName());
        return command;
    }

    /**
     * @deprecated 로컬 개발 환경 전용 Docker fallback이다. 3D 변환 바이너리 경로가 준비되면 삭제한다.
     */
    @Deprecated(forRemoval = false)
    private List<String> buildLocalPngCommand(Path inputPath, Path outputPath, Path workDir, Path settingsPath) {
        final String containerWorkDir = "/data";
        final String localThreeDConverterImage = "fabbit-3dconverter:latest";

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-v");
        command.add(workDir.toAbsolutePath() + ":" + containerWorkDir);
        command.add("--entrypoint");
        command.add("/bin/sh");
        command.add(localThreeDConverterImage);
        command.add("-lc");
        command.add(
                "xvfb-run --auto-servernum /opt/mayo-conv "
                        + containerWorkDir + "/" + inputPath.getFileName()
                        + " -u "
                        + containerWorkDir + "/" + settingsPath.getFileName()
                        + " -e "
                        + containerWorkDir + "/" + outputPath.getFileName()
        );
        return command;
    }
}
