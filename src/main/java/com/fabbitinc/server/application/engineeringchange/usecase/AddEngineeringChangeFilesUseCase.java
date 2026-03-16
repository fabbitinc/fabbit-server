package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.workitem.usecase.result.AttachedFileResult;
import com.fabbitinc.server.domain.file.model.File;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class AddEngineeringChangeFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final FileService fileService;
    private final FileUrlResolver fileUrlResolver;

    public List<AttachedFileResult> execute(AddEngineeringChangeFilesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<File> attachedFiles = engineeringChangeService.attachFiles(
                auth.userId(),
                WorkItemUseCaseSupport.resolveEngineeringChangeId(engineeringChangeService, command.engineeringChangeNumber()),
                fileService.validateAttachable(command.fileIds())
        );
        return attachedFiles.stream()
                .map(file -> new AttachedFileResult(
                        file.getId(),
                        file.getOriginalName(),
                        file.getContentType(),
                        file.getFileSize(),
                        fileUrlResolver.resolve(file.getFileKey()),
                        file.getCreatedAt()
                ))
                .toList();
    }

    public record AddEngineeringChangeFilesCommand(
            int engineeringChangeNumber,
            List<UUID> fileIds
    ) {
        public AddEngineeringChangeFilesCommand {
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
        }
    }
}
