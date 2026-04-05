package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartNumberSequenceRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 카테고리 채번 규칙에 따라 품번을 자동 생성하는 서비스.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartNumberService {

    private static final int MAX_RETRY = 3;

    private final PartCategoryRepository categoryRepository;
    private final PartNumberSequenceRepository sequenceRepository;
    private final PartRepository partRepository;

    /**
     * 지정된 카테고리의 채번 규칙에 따라 다음 품번을 생성한다.
     * 비관적 잠금으로 동시성을 제어하며, 품번 충돌 시 최대 3회 재시도한다.
     */
    public String generate(UUID categoryId) {
        PartCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "카테고리를 찾을 수 없습니다: " + categoryId));

        PartNumberSequence sequence = sequenceRepository.findByCategoryIdForUpdate(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "채번 시퀀스를 찾을 수 없습니다: " + categoryId));

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            int nextVal = sequence.nextValue();

            if (category.isSequenceExhausted(nextVal)) {
                throw new AppException(ErrorCode.CONFLICT,
                        "시퀀스 최대값에 도달했습니다. 카테고리 '%s'의 자릿수를 확장해 주세요".formatted(category.getName()));
            }

            String partNumber = category.formatNumber(nextVal);

            if (partRepository.findByPartNumber(partNumber).isEmpty()) {
                log.info("채번 성공: category={}, partNumber={}", category.getName(), partNumber);
                return partNumber;
            }

            log.warn("채번 충돌 발생: partNumber={}, 재시도 {}/{}", partNumber, attempt + 1, MAX_RETRY);
        }

        throw new AppException(ErrorCode.CONFLICT, "채번 충돌이 반복되었습니다. 잠시 후 다시 시도해 주세요");
    }
}
