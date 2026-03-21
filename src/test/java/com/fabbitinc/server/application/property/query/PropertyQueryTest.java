package com.fabbitinc.server.application.property.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaListCondition;
import com.fabbitinc.server.application.property.query.result.PropertyMetaListResult;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyQueryTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;

    @Mock
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void list_통합_catalog_row를_정렬해서_반환한다() {
        PropertyDefinition system = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "category",
                PartSystemPropertyKind.CATEGORY,
                "품목군",
                "부품 분류",
                PropertyValueType.STRING,
                null,
                List.of(),
                "category",
                65,
                false,
                true
        );
        PropertyDefinition custom = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                "표면처리 방식",
                PropertyValueType.STRING,
                null,
                List.of(),
                75,
                false
        );
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                UUID.randomUUID(),
                "test@gmail.com",
                UUID.randomUUID(),
                MembershipRole.ADMIN
        ));
        when(propertyDefinitionRepository.findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
                PropertyOwnerType.PART
        )).thenReturn(List.of(system, custom));

        PropertyQuery query = new PropertyQuery(currentAuthProvider, propertyDefinitionRepository);

        PropertyMetaListResult result = query.list(new PropertyMetaListCondition(PropertyOwnerType.PART.name(), false));

        assertEquals("category", result.items().get(0).propertyKey());
        assertEquals(true, result.items().get(0).system());
        assertEquals("category", result.items().get(0).columnName());
        assertEquals(custom.getPropertyKey(), result.items().get(1).propertyKey());
        assertEquals(false, result.items().get(1).system());
        assertEquals(PartSystemPropertyKind.CATEGORY, result.items().get(0).partSystemPropertyKind());
    }
}
