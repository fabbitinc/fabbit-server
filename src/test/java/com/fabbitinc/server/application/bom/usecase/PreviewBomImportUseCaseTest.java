package com.fabbitinc.server.application.bom.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.usecase.command.PreviewBomImportCommand;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreviewBomImportUseCaseTest {

    @Test
    void execute_partId와_revisionId가_일치하지_않으면_파일을_읽지_않는다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        FileRepository fileRepository = mock(FileRepository.class);
        StoragePort storagePort = mock(StoragePort.class);
        SpreadsheetParserSupport spreadsheetParserSupport = mock(SpreadsheetParserSupport.class);
        PartRevisionRepository partRevisionRepository = mock(PartRevisionRepository.class);
        EngineeringBomItemRepository engineeringBomItemRepository = mock(EngineeringBomItemRepository.class);

        PreviewBomImportUseCase useCase = new PreviewBomImportUseCase(
                currentAuthProvider,
                fileRepository,
                storagePort,
                spreadsheetParserSupport,
                partRevisionRepository,
                engineeringBomItemRepository
        );

        UUID actorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(actorId, "a@b.c", orgId, null));
        when(partRevisionRepository.findByIdAndPartId(revisionId, partId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> useCase.execute(new PreviewBomImportCommand(partId, revisionId, fileId)));

        verifyNoInteractions(fileRepository, storagePort, spreadsheetParserSupport, engineeringBomItemRepository);
    }
}
