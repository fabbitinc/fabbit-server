package com.fabbitinc.server.application.property.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaListCondition;
import com.fabbitinc.server.application.property.query.result.PropertyMetaListResult;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.repository.SystemPropertyOverrideRepository;
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

    @Mock
    private SystemPropertyOverrideRepository systemPropertyOverrideRepository;

    @Test
    void list_시스템속성과_커스텀속성을_합쳐서_정렬한다() {
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
        SystemPropertyOverride override = SystemPropertyOverride.create(
                PropertyOwnerType.PART,
                "category",
                "품목군",
                65,
                true
        );
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                UUID.randomUUID(),
                "test@gmail.com",
                UUID.randomUUID(),
                MembershipRole.ADMIN
        ));
        when(systemPropertyOverrideRepository.findByOwnerTypeOrderByDisplayOrderAscPropertyKeyAsc(
                PropertyOwnerType.PART
        )).thenReturn(List.of(override));
        when(propertyDefinitionRepository.findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
                PropertyOwnerType.PART
        )).thenReturn(List.of(custom));

        PropertyQuery query = new PropertyQuery(
                currentAuthProvider,
                propertyDefinitionRepository,
                systemPropertyOverrideRepository
        );

        PropertyMetaListResult result = query.list(new PropertyMetaListCondition(PropertyOwnerType.PART.name(), false));

        assertEquals("part_number", result.items().get(0).propertyKey());
        assertEquals(false, result.items().get(0).activeConfigurable());
        assertEquals("품목군", result.items().stream()
                .filter(item -> item.propertyKey().equals("category"))
                .findFirst()
                .orElseThrow()
                .displayName());
        assertEquals(custom.getId().toString(), result.items().stream()
                .filter(item -> !item.system())
                .findFirst()
                .orElseThrow()
                .propertyKey());
        assertEquals(PartSystemPropertyKind.CATEGORY, result.items().stream()
                .filter(item -> item.propertyKey().equals("category"))
                .findFirst()
                .orElseThrow()
                .partSystemPropertyKind());
        assertEquals(true, result.items().stream()
                .filter(item -> item.propertyKey().equals("category"))
                .findFirst()
                .orElseThrow()
                .activeConfigurable());
    }

    @Test
    void list_includeInactive가_false면_비활성_커스텀속성을_제외한다() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                UUID.randomUUID(),
                "test@gmail.com",
                UUID.randomUUID(),
                MembershipRole.ADMIN
        ));
        when(systemPropertyOverrideRepository.findByOwnerTypeOrderByDisplayOrderAscPropertyKeyAsc(
                PropertyOwnerType.PART
        )).thenReturn(List.of());
        when(propertyDefinitionRepository.findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
                PropertyOwnerType.PART
        )).thenReturn(List.of());

        PropertyQuery query = new PropertyQuery(
                currentAuthProvider,
                propertyDefinitionRepository,
                systemPropertyOverrideRepository
        );

        PropertyMetaListResult result = query.list(new PropertyMetaListCondition(PropertyOwnerType.PART.name(), false));

        assertEquals(true, result.items().stream().noneMatch(item -> "내부코드".equals(item.displayName())));
    }
}
