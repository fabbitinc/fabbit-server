package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddIssueFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final FileService fileService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public List<FileItemResponse> execute(IssueTargetType targetType,
            int issueNumber,
            AttachFilesRequest request
    ) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = resolveIssueId(targetType, issueNumber);

        List<File> attachableFiles = fileService.validateAttachable(request.fileIds());
        List<File> attachedFiles = issueService.attachFiles(auth.userId(), issueId, attachableFiles);
        return attachedFiles.stream()
                .map(file -> new FileItemResponse(
                        file.getId(),
                        file.getOriginalName(),
                        file.getContentType(),
                        file.getFileSize(),
                        fileUrlResolver.resolve(file.getFileKey()),
                        file.getCreatedAt()
                ))
                .toList();
    }

    private UUID resolveIssueId(IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }
}
