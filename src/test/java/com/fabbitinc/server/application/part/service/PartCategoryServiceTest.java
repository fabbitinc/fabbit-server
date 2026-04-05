package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PartCategoryServiceTest {

    @Mock private PartCategoryRepository partCategoryRepository;
    @Mock private PartNumberSequenceRepository partNumberSequenceRepository;
    @Mock private PartRepository partRepository;

    @Test
    void create_카테고리와_시퀀스를_함께_생성한다() {
        PartCategory category = PartCategory.create("PCB", "PCB", "-", 4);
        when(partCategoryRepository.existsByName("PCB")).thenReturn(false);
        when(partCategoryRepository.existsByPrefix("PCB")).thenReturn(false);
        when(partCategoryRepository.save(any(PartCategory.class))).thenReturn(category);

        PartCategoryService service = createService();
        PartCategory result = service.create("PCB", "PCB", "-", 4);

        assertEquals("PCB", result.getName());
        verify(partNumberSequenceRepository).save(any(PartNumberSequence.class));
    }

    @Test
    void delete_사용중인_카테고리면_conflict를_던진다() {
        UUID categoryId = UUID.randomUUID();
        PartCategory category = PartCategory.create("PCB", "PCB", "-", 4);
        when(partCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partRepository.countByCategoryId(categoryId)).thenReturn(1L);

        PartCategoryService service = createService();

        AppException ex = assertThrows(AppException.class, () -> service.delete(categoryId));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void delete_삭제직전_fk충돌이발생해도_conflict로매핑한다() {
        UUID categoryId = UUID.randomUUID();
        PartCategory category = PartCategory.create("PCB", "PCB", "-", 4);
        PartNumberSequence sequence = PartNumberSequence.createFor(categoryId);
        when(partCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partRepository.countByCategoryId(categoryId)).thenReturn(0L);
        when(partNumberSequenceRepository.findByCategoryIdForUpdate(categoryId)).thenReturn(Optional.of(sequence));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk"))
                .when(partCategoryRepository)
                .delete(category);

        PartCategoryService service = createService();

        AppException ex = assertThrows(AppException.class, () -> service.delete(categoryId));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    private PartCategoryService createService() {
        return new PartCategoryService(
                partCategoryRepository,
                partNumberSequenceRepository,
                partRepository
        );
    }
}
