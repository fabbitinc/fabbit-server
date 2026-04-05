package com.fabbitinc.server.presentation.part.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.part.model.PartItemType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreatePartRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void categoryId와_itemType이_없으면_검증실패한다() {
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

        assertEquals(2, violations.size());
    }

    @Test
    void blank_partNumber여도_categoryId와_itemType이_있으면_검증성공한다() {
        CreatePartRequest request = new CreatePartRequest(
                "   ",
                UUID.randomUUID(),
                PartItemType.MANUFACTURED,
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
