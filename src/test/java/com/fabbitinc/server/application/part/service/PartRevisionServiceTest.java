package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.part.service.input.PartRevisionDecisionInput;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PartRevisionServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private PartRevisionRepository partRevisionRepository;

    @Test
    void cancelDraft_revisionScopedDraft도_CANCELED로_전환한다() {
        Part part = Part.create("AES-100");
        PartRevision baseRevision = PartRevision.createOfficial(part, "1", null, "본체", PartRevisionStatus.RELEASED, null);
        PartRevision draft = PartRevision.createDraft(part, "D1", baseRevision.getId(), "개정 초안", null);

        when(partRevisionRepository.findByPartNumberAndRevisionCode("AES-100", "1"))
                .thenReturn(Optional.of(baseRevision));
        when(partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionId("AES-100", "D1", baseRevision.getId()))
                .thenReturn(Optional.of(draft));

        PartRevisionService service = new PartRevisionService(partRepository, partRevisionRepository, new ObjectMapper());

        PartRevision canceled = service.cancelDraft(
                new PartRevisionDecisionInput("AES-100", "1", "D1", "초안 폐기"),
                UUID.randomUUID()
        );

        assertEquals(PartRevisionStatus.CANCELED, canceled.getStatus());
        assertEquals(null, canceled.getRevisionCode());
        assertEquals(null, canceled.getDraftKey());
        assertEquals(PartRevisionHistoryActionType.CANCELED, canceled.getHistories().getLast().getActionType());
    }
}
