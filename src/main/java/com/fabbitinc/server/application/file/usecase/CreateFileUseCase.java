package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.file.service.input.CreateFileInput;
import com.fabbitinc.server.application.file.service.output.CreateFileOutput;
import com.fabbitinc.server.application.file.usecase.command.CreateFileCommand;
import com.fabbitinc.server.application.file.usecase.result.CreatedFileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    public CreatedFileResult execute(CreateFileCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        CreateFileOutput output = fileService.createFile(
                auth,
                new CreateFileInput(
                        command.originalName(),
                        command.contentType(),
                        command.fileSize(),
                        command.contentHash()
                )
        );
        return new CreatedFileResult(output.fileId(), output.uploadUrl(), output.fileKey());
    }
}
