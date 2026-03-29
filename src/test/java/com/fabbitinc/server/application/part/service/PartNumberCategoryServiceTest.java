package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartNumberCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartNumberCategoryServiceTest {

    @Mock private PartNumberCategoryRepository partNumberCategoryRepository;
    @Mock private PartNumberSequenceRepository partNumberSequenceRepository;
    @Mock private PartRepository partRepository;

    @Test
    void create_카테고리와_시퀀스를_함께_생성한다() {
        PartNumberCategory category = PartNumberCategory.create("PCB", "PCB", "-", 4);
        when(partNumberCategoryRepository.existsByName("PCB")).thenReturn(false);
        when(partNumberCategoryRepository.existsByPrefix("PCB")).thenReturn(false);
        when(partNumberCategoryRepository.save(any(PartNumberCategory.class))).thenReturn(category);

        PartNumberCategoryService service = createService();
        PartNumberCategory result = service.create("PCB", "PCB", "-", 4);

        assertEquals("PCB", result.getName());
        verify(partNumberSequenceRepository).save(any(PartNumberSequence.class));
    }

    @Test
    void delete_사용중인_카테고리면_conflict를_던진다() {
        UUID categoryId = UUID.randomUUID();
        PartNumberCategory category = PartNumberCategory.create("PCB", "PCB", "-", 4);
        when(partNumberCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(partRepository.countByNumberingCategoryId(categoryId)).thenReturn(1L);

        PartNumberCategoryService service = createService();

        AppException ex = assertThrows(AppException.class, () -> service.delete(categoryId));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    private PartNumberCategoryService createService() {
        return new PartNumberCategoryService(
                partNumberCategoryRepository,
                partNumberSequenceRepository,
                partRepository
        );
    }
}
