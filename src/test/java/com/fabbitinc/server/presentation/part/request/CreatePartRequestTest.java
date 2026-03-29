package com.fabbitinc.server.presentation.part.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CreatePartRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void partNumber와_numberingCategoryId가_둘다없으면_검증실패한다() {
        CreatePartRequest request = new CreatePartRequest(
                null,
                null,
                null,
                "Bolt",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("part_number 또는 numbering_category_id 중 하나는 필수입니다"));
    }

    @Test
    void blank_partNumber와_numberingCategoryId가_둘다없으면_검증실패한다() {
        CreatePartRequest request = new CreatePartRequest(
                "   ",
                null,
                null,
                "Bolt",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    @Test
    void blank_partNumber여도_numberingCategoryId가있으면_검증성공한다() {
        CreatePartRequest request = new CreatePartRequest(
                "   ",
                java.util.UUID.randomUUID(),
                null,
                "Bolt",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
