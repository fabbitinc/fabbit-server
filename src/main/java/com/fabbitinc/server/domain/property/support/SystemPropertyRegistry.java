package com.fabbitinc.server.domain.property.support;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.List;
import java.util.Optional;

public final class SystemPropertyRegistry {

    private static final List<SystemPropertySpec> SPECS = List.of(
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "part_number",
                    PartSystemPropertyKind.PART_NUMBER,
                    "품번",
                    "부품의 고유 식별자",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "part_number",
                    10,
                    true,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "name",
                    PartSystemPropertyKind.NAME,
                    "품명",
                    "사람이 읽을 수 있는 부품 이름",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "name",
                    20,
                    false,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "revision",
                    PartSystemPropertyKind.REVISION,
                    "리비전",
                    "부품 리비전",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "revision",
                    30,
                    false,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "material",
                    PartSystemPropertyKind.MATERIAL,
                    "재질",
                    "부품 재질",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "material",
                    40,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "unit",
                    PartSystemPropertyKind.UNIT,
                    "단위",
                    "부품 수량 단위",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "unit",
                    50,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "description",
                    PartSystemPropertyKind.DESCRIPTION,
                    "설명",
                    "부품 설명",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "description",
                    60,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "category",
                    PartSystemPropertyKind.CATEGORY,
                    "카테고리",
                    "부품 분류",
                    PropertyValueType.OPTION,
                    PropertyOptionMode.CREATABLE,
                    List.of(),
                    "category",
                    70,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "is_phantom",
                    PartSystemPropertyKind.PHANTOM,
                    "팬텀 여부",
                    "팬텀 조립품 여부",
                    PropertyValueType.BOOLEAN,
                    null,
                    List.of(),
                    "is_phantom",
                    80,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "lifecycle_state",
                    PartSystemPropertyKind.LIFECYCLE_STATE,
                    "수명주기 상태",
                    "부품 수명주기 상태",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "lifecycle_state",
                    90,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART,
                    "lead_time_days",
                    PartSystemPropertyKind.LEAD_TIME_DAYS,
                    "리드타임(일)",
                    "부품 리드타임(일)",
                    PropertyValueType.INTEGER,
                    null,
                    List.of(),
                    "lead_time_days",
                    100,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.SUPPLIER,
                    "company_name",
                    null,
                    "공급사명",
                    "공급사 회사명",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "company_name",
                    10,
                    true,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.SUPPLIER,
                    "code",
                    null,
                    "공급사 코드",
                    "내부 식별용 공급사 코드",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "code",
                    20,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.SUPPLIER,
                    "country",
                    null,
                    "국가",
                    "공급사 국가",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "country",
                    30,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.SUPPLIER,
                    "contact_info",
                    null,
                    "연락처",
                    "공급사 연락처 정보",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "contact_info",
                    40,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "drawing_number",
                    null,
                    "도면번호",
                    "도면의 식별 번호",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "drawing_number",
                    10,
                    false,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "name",
                    null,
                    "도면명",
                    "사람이 읽을 수 있는 도면 이름",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "name",
                    20,
                    true,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "version",
                    null,
                    "버전",
                    "도면 버전",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "version",
                    30,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "status",
                    null,
                    "상태",
                    "도면 상태",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "status",
                    40,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "conversion_status",
                    null,
                    "변환 상태",
                    "도면 변환 상태",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "conversion_status",
                    50,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "source_type",
                    null,
                    "소스 타입",
                    "도면 소스 타입",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "source_type",
                    60,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.DRAWING,
                    "dimension",
                    null,
                    "차원",
                    "도면 차원 정보",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "dimension",
                    70,
                    false,
                    true
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.BOM_LINK,
                    "quantity",
                    null,
                    "수량",
                    "BOM 관계 수량",
                    PropertyValueType.FLOAT,
                    null,
                    List.of(),
                    "quantity",
                    10,
                    true,
                    false
            ),
            new SystemPropertySpec(
                    PropertyOwnerType.PART_SUPPLIER,
                    "unit_cost",
                    null,
                    "단가",
                    "공급사별 부품 단가",
                    PropertyValueType.FLOAT,
                    null,
                    List.of(),
                    "unit_cost",
                    10,
                    false,
                    true
            )
    );

    private SystemPropertyRegistry() {
    }

    public static List<SystemPropertySpec> list() {
        return SPECS;
    }

    public static List<SystemPropertySpec> listByOwnerType(PropertyOwnerType ownerType) {
        return SPECS.stream()
                .filter(spec -> spec.ownerType() == ownerType)
                .toList();
    }

    public static Optional<SystemPropertySpec> find(PropertyOwnerType ownerType, String propertyKey) {
        return SPECS.stream()
                .filter(spec -> spec.ownerType() == ownerType && spec.propertyKey().equals(propertyKey))
                .findFirst();
    }
}
