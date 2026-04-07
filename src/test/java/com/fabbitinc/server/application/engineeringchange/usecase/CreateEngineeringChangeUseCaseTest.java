package com.fabbitinc.server.application.engineeringchange.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateEngineeringChangeUseCaseTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private FileService fileService;
    @Mock
    private EngineeringChangeService engineeringChangeService;
    @Mock
    private SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase;

    @InjectMocks
    private CreateEngineeringChangeUseCase useCase;

    @Test
    void createEngineeringChange_sourceIssue와_linkedIssues를_합쳐동기화한다() {
        UUID actorId = UUID.randomUUID();
        UUID engineeringChangeId = UUID.randomUUID();
        UUID sourceIssueId = UUID.randomUUID();
        UUID linkedIssueId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "EC", "본문", sourceIssueId, actorId);
        org.springframework.test.util.ReflectionTestUtils.setField(engineeringChange, "id", engineeringChangeId);

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(engineeringChangeService.createEngineeringChange(actorId, "EC", null, sourceIssueId)).thenReturn(engineeringChange);

        CreateEngineeringChangeUseCase.CreateEngineeringChangeResult result = useCase.execute(
                new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand(
                        "EC",
                        null,
                        sourceIssueId,
                        List.of(linkedIssueId),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        assertEquals(engineeringChangeId, result.engineeringChangeId());
        verify(engineeringChangeService).syncIssues(
                actorId,
                engineeringChangeId,
                List.of(sourceIssueId, linkedIssueId),
                false
        );
    }
}
