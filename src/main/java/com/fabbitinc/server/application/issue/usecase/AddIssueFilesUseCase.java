package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.result.AttachedFileResult;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional
@RequiredArgsConstructor
public class AddIssueFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final FileService fileService;
    private final FileUrlResolver fileUrlResolver;

    public List<AttachedFileResult> execute(AddIssueFilesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.targetType(), command.issueNumber());

        List<File> attachableFiles = fileService.validateAttachable(command.fileIds());
        List<File> attachedFiles = issueService.attachFiles(auth.userId(), issueId, attachableFiles);
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

    public record AddIssueFilesCommand(
            IssueTargetType targetType,
            int issueNumber,
            List<UUID> fileIds
    ) {
        public AddIssueFilesCommand {
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
        }
    }
}
