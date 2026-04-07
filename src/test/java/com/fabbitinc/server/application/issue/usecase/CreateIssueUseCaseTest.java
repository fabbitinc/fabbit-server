package com.fabbitinc.server.application.issue.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateIssueUseCaseTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private FileService fileService;
    @Mock
    private IssueService issueService;

    @InjectMocks
    private CreateIssueUseCase useCase;

    @Test
    void createIssue_연결된_변경관리를_함께동기화한다() {
        UUID actorId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();
        UUID engineeringChangeId = UUID.randomUUID();
        Issue issue = Issue.create(1, "이슈", "본문", actorId);
        org.springframework.test.util.ReflectionTestUtils.setField(issue, "id", issueId);

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(issueService.createIssue(actorId, "이슈", null)).thenReturn(issue);

        CreateIssueUseCase.CreateIssueResult result = useCase.execute(
                new CreateIssueUseCase.CreateIssueCommand(
                        "이슈",
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(engineeringChangeId),
                        List.of()
                )
        );

        assertEquals(issueId, result.issueId());
        verify(issueService).syncLinkedEngineeringChanges(actorId, issueId, List.of(engineeringChangeId), false);
    }
}
