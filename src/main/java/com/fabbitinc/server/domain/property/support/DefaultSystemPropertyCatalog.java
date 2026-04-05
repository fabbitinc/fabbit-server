package com.fabbitinc.server.domain.property.support;

import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.List;

public final class DefaultSystemPropertyCatalog {

    private static final List<SystemPropertyCatalogSeed> ITEMS = List.of(
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "part_number",
                    PartSystemPropertyKind.PART_NUMBER,
                    "품번",
                    "부품의 고유 식별자",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "part_number",
                    1,
                    true,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "name",
                    PartSystemPropertyKind.NAME,
                    "품명",
                    "사람이 읽을 수 있는 부품 이름",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "name",
                    2,
                    false,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "revision",
                    PartSystemPropertyKind.REVISION,
                    "리비전",
                    "부품 리비전",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "revision_code",
                    3,
                    false,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "material",
                    PartSystemPropertyKind.MATERIAL,
                    "재질",
                    "부품 재질",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "material",
                    4,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "unit",
                    PartSystemPropertyKind.UNIT,
                    "단위",
                    "부품 수량 단위",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "unit",
                    5,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "description",
                    PartSystemPropertyKind.DESCRIPTION,
                    "설명",
                    "부품 설명",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "description",
                    6,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "item_type",
                    PartSystemPropertyKind.ITEM_TYPE,
                    "아이템 유형",
                    "부품 아이템 유형",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "item_type",
                    7,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "lifecycle_state",
                    PartSystemPropertyKind.LIFECYCLE_STATE,
                    "수명주기 상태",
                    "부품 수명주기 상태",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "lifecycle_state",
                    8,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART,
                    "lead_time_days",
                    PartSystemPropertyKind.LEAD_TIME_DAYS,
                    "리드타임(일)",
                    "부품 리드타임(일)",
                    PropertyValueType.INTEGER,
                    null,
                    List.of(),
                    "lead_time_days",
                    9,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.SUPPLIER,
                    "company_name",
                    null,
                    "공급사명",
                    "공급사 회사명",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "company_name",
                    1,
                    true,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.SUPPLIER,
                    "code",
                    null,
                    "공급사 코드",
                    "내부 식별용 공급사 코드",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "code",
                    2,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.SUPPLIER,
                    "country",
                    null,
                    "국가",
                    "공급사 국가",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "country",
                    3,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.SUPPLIER,
                    "contact_info",
                    null,
                    "연락처",
                    "공급사 연락처 정보",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "contact_info",
                    4,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "drawing_number",
                    null,
                    "도면번호",
                    "도면의 식별 번호",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "drawing_number",
                    1,
                    false,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "name",
                    null,
                    "도면명",
                    "사람이 읽을 수 있는 도면 이름",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "name",
                    2,
                    true,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "version",
                    null,
                    "버전",
                    "도면 버전",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "version",
                    3,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "status",
                    null,
                    "상태",
                    "도면 상태",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "status",
                    4,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "dimension",
                    null,
                    "규격",
                    "도면 규격",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "dimension",
                    5,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "source_type",
                    null,
                    "원본 유형",
                    "도면 원본 유형",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "source_type",
                    6,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.DRAWING,
                    "original_file_key",
                    null,
                    "원본 파일 키",
                    "원본 파일 저장 키",
                    PropertyValueType.STRING,
                    null,
                    List.of(),
                    "original_file_key",
                    7,
                    false,
                    true
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.BOM_LINK,
                    "quantity",
                    null,
                    "수량",
                    "BOM 링크 수량",
                    PropertyValueType.FLOAT,
                    null,
                    List.of(),
                    "quantity",
                    1,
                    true,
                    false
            ),
            new SystemPropertyCatalogSeed(
                    PropertyOwnerType.PART_SUPPLIER,
                    "unit_cost",
                    null,
                    "단가",
                    "부품-공급사 단가",
                    PropertyValueType.FLOAT,
                    null,
                    List.of(),
                    "unit_cost",
                    1,
                    false,
                    true
            )
    );

    private DefaultSystemPropertyCatalog() {
    }

    public static List<SystemPropertyCatalogSeed> items() {
        return ITEMS;
    }

    public static List<SystemPropertyCatalogSeed> itemsOf(PropertyOwnerType ownerType) {
        return ITEMS.stream()
                .filter(item -> item.ownerType() == ownerType)
                .toList();
    }
}
