package com.fabbitinc.server.application.part.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.query.result.PartNumberAvailabilityResult;
import com.fabbitinc.server.application.part.query.result.PartNumberPreviewResult;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
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
class PartCategoryQueryTest {

    @Mock private CurrentAuthProvider currentAuthProvider;
    @Mock private PartCategoryRepository partCategoryRepository;
    @Mock private PartNumberSequenceRepository partNumberSequenceRepository;
    @Mock private PartRepository partRepository;

    @Test
    void get_다음_품번_미리보기를_반환한다() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));

        PartCategory category = PartCategory.create("PCB", "PCB-", "", 4, true);
        PartNumberSequence sequence = PartNumberSequence.createFor(categoryId);
        setCurrentValue(sequence, 41);

        when(partCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partNumberSequenceRepository.findByCategoryId(categoryId)).thenReturn(Optional.of(sequence));

        PartCategoryQuery query = createQuery();
        PartNumberPreviewResult result = query.get(categoryId);

        assertEquals("PCB-0042", result.partNumber());
        assertEquals("이 번호는 실제 생성 시 변경될 수 있습니다", result.note());
    }

    @Test
    void lookup_품번_사용가능여부를_반환한다() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));
        when(partRepository.findByPartNumber("PCB-0042")).thenReturn(Optional.empty());

        PartCategoryQuery query = createQuery();
        PartNumberAvailabilityResult result = query.lookup("  PCB-0042  ");

        assertEquals("PCB-0042", result.partNumber());
        assertTrue(result.available());
    }

    @Test
    void lookup_이미_존재하는_품번이면_false를_반환한다() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));
        when(partRepository.findByPartNumber("PCB-0001")).thenReturn(Optional.of(Part.create("PCB-0001")));

        PartCategoryQuery query = createQuery();
        PartNumberAvailabilityResult result = query.lookup("PCB-0001");

        assertFalse(result.available());
    }

    @Test
    void get_시퀀스가_소진되면_conflict를_던진다() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));

        PartCategory category = PartCategory.create("PCB", "PCB-", "", 4, true);
        PartNumberSequence sequence = PartNumberSequence.createFor(categoryId);
        setCurrentValue(sequence, 9999);

        when(partCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partNumberSequenceRepository.findByCategoryId(categoryId)).thenReturn(Optional.of(sequence));

        PartCategoryQuery query = createQuery();
        AppException ex = assertThrows(AppException.class, () -> query.get(categoryId));

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    private PartCategoryQuery createQuery() {
        return new PartCategoryQuery(
                currentAuthProvider,
                partCategoryRepository,
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
