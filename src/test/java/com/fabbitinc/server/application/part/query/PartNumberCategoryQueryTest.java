package com.fabbitinc.server.application.part.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.query.result.PartNumberAvailabilityResult;
import com.fabbitinc.server.application.part.query.result.PartNumberPreviewResult;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartNumberCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartNumberCategoryQueryTest {

    @Mock private CurrentAuthProvider currentAuthProvider;
    @Mock private PartNumberCategoryRepository partNumberCategoryRepository;
    @Mock private PartNumberSequenceRepository partNumberSequenceRepository;
    @Mock private PartRepository partRepository;

    @Test
    void get_다음_품번_미리보기를_반환한다() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));

        PartNumberCategory category = PartNumberCategory.create("PCB", "PCB", "-", 4);
        PartNumberSequence sequence = PartNumberSequence.createFor(categoryId);
        setCurrentValue(sequence, 41);

        when(partNumberCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partNumberSequenceRepository.findByCategoryId(categoryId)).thenReturn(Optional.of(sequence));

        PartNumberCategoryQuery query = createQuery();
        PartNumberPreviewResult result = query.get(categoryId);

        assertEquals("PCB-0042", result.partNumber());
        assertEquals("이 번호는 실제 생성 시 변경될 수 있습니다", result.note());
    }

    @Test
    void lookup_품번_사용가능여부를_반환한다() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));
        when(partRepository.findByPartNumber("PCB-0042")).thenReturn(Optional.empty());

        PartNumberCategoryQuery query = createQuery();
        PartNumberAvailabilityResult result = query.lookup("  PCB-0042  ");

        assertEquals("PCB-0042", result.partNumber());
        assertTrue(result.available());
    }

    @Test
    void lookup_이미_존재하는_품번이면_false를_반환한다() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));
        when(partRepository.findByPartNumber("PCB-0001")).thenReturn(Optional.of(Part.create("PCB-0001")));

        PartNumberCategoryQuery query = createQuery();
        PartNumberAvailabilityResult result = query.lookup("PCB-0001");

        assertFalse(result.available());
    }

    private PartNumberCategoryQuery createQuery() {
        return new PartNumberCategoryQuery(
                currentAuthProvider,
                partNumberCategoryRepository,
                partNumberSequenceRepository,
                partRepository
        );
    }

    private void setCurrentValue(PartNumberSequence sequence, int value) throws Exception {
        Field field = PartNumberSequence.class.getDeclaredField("currentValue");
        field.setAccessible(true);
        field.set(sequence, value);
    }
}
