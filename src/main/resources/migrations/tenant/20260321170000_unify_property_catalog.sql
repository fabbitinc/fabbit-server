-- liquibase formatted sql

-- changeset codex:20260321170000-1 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "property_key" VARCHAR(100);

-- changeset codex:20260321170000-2 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "source_type" VARCHAR(20);

-- changeset codex:20260321170000-3 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "storage_kind" VARCHAR(30);

-- changeset codex:20260321170000-4 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "storage_binding" VARCHAR(200);

-- changeset codex:20260321170000-5 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "part_system_property_kind" VARCHAR(50);

-- changeset codex:20260321170000-6 splitStatements:false
ALTER TABLE "property_definitions" ADD COLUMN "is_active_configurable" BOOLEAN;

-- changeset codex:20260321170000-7 splitStatements:false
UPDATE "property_definitions"
SET
    "property_key" = CAST("id" AS VARCHAR(100)),
    "source_type" = 'CUSTOM',
    "storage_kind" = 'EXTENDED_PROPERTY',
    "storage_binding" = CAST("id" AS VARCHAR(200)),
    "is_active_configurable" = TRUE
WHERE "property_key" IS NULL;

-- changeset codex:20260321170000-8 splitStatements:false
ALTER TABLE "property_definitions" ALTER COLUMN "property_key" SET NOT NULL;

-- changeset codex:20260321170000-9 splitStatements:false
ALTER TABLE "property_definitions" ALTER COLUMN "source_type" SET NOT NULL;

-- changeset codex:20260321170000-10 splitStatements:false
ALTER TABLE "property_definitions" ALTER COLUMN "storage_kind" SET NOT NULL;

-- changeset codex:20260321170000-11 splitStatements:false
ALTER TABLE "property_definitions" ALTER COLUMN "storage_binding" SET NOT NULL;

-- changeset codex:20260321170000-12 splitStatements:false
ALTER TABLE "property_definitions" ALTER COLUMN "is_active_configurable" SET NOT NULL;

-- changeset codex:20260321170000-13 splitStatements:false
ALTER TABLE "property_definitions" DROP CONSTRAINT "uq_property_definitions_owner_type_display_name";

-- changeset codex:20260321170000-14 splitStatements:false
ALTER TABLE "property_definitions" ADD CONSTRAINT "uq_property_definitions_owner_type_property_key"
UNIQUE ("owner_type", "property_key");

-- changeset codex:20260321170000-15 splitStatements:false
INSERT INTO "property_definitions" (
    "id",
    "created_at",
    "updated_at",
    "owner_type",
    "property_key",
    "source_type",
    "storage_kind",
    "storage_binding",
    "part_system_property_kind",
    "display_name",
    "description",
    "value_type",
    "option_mode",
    "options_json",
    "display_order",
    "is_required",
    "is_active",
    "is_active_configurable"
)
SELECT
    (
        SUBSTRING(MD5(seed.owner_type || ':' || seed.property_key), 1, 8) || '-' ||
        SUBSTRING(MD5(seed.owner_type || ':' || seed.property_key), 9, 4) || '-' ||
        SUBSTRING(MD5(seed.owner_type || ':' || seed.property_key), 13, 4) || '-' ||
        SUBSTRING(MD5(seed.owner_type || ':' || seed.property_key), 17, 4) || '-' ||
        SUBSTRING(MD5(seed.owner_type || ':' || seed.property_key), 21, 12)
    )::UUID,
    NOW(),
    NOW(),
    seed.owner_type,
    seed.property_key,
    'SYSTEM',
    'COLUMN',
    seed.storage_binding,
    seed.part_system_property_kind,
    COALESCE(overrides.display_name_override, seed.display_name),
    seed.description,
    seed.value_type,
    seed.option_mode,
    seed.options_json::JSONB,
    COALESCE(overrides.display_order, seed.display_order),
    seed.is_required,
    COALESCE(overrides.is_active, TRUE),
    seed.is_active_configurable
FROM (
    VALUES
        ('PART', 'part_number', 'PART_NUMBER', '품번', '부품의 고유 식별자', 'STRING', NULL, '[]', 'part_number', 1, TRUE, FALSE),
        ('PART', 'name', 'NAME', '품명', '사람이 읽을 수 있는 부품 이름', 'STRING', NULL, '[]', 'name', 2, FALSE, FALSE),
        ('PART', 'revision', 'REVISION', '리비전', '부품 리비전', 'STRING', NULL, '[]', 'revision_code', 3, FALSE, FALSE),
        ('PART', 'material', 'MATERIAL', '재질', '부품 재질', 'STRING', NULL, '[]', 'material', 4, FALSE, TRUE),
        ('PART', 'unit', 'UNIT', '단위', '부품 수량 단위', 'STRING', NULL, '[]', 'unit', 5, FALSE, TRUE),
        ('PART', 'description', 'DESCRIPTION', '설명', '부품 설명', 'STRING', NULL, '[]', 'description', 6, FALSE, TRUE),
        ('PART', 'category', 'CATEGORY', '카테고리', '부품 분류', 'OPTION', 'CREATABLE', '[]', 'category', 7, FALSE, TRUE),
        ('PART', 'is_phantom', 'PHANTOM', '팬텀 여부', '팬텀 조립품 여부', 'BOOLEAN', NULL, '[]', 'is_phantom', 8, FALSE, TRUE),
        ('PART', 'lifecycle_state', 'LIFECYCLE_STATE', '수명주기 상태', '부품 수명주기 상태', 'STRING', NULL, '[]', 'lifecycle_state', 9, FALSE, TRUE),
        ('PART', 'lead_time_days', 'LEAD_TIME_DAYS', '리드타임(일)', '부품 리드타임(일)', 'INTEGER', NULL, '[]', 'lead_time_days', 10, FALSE, TRUE),
        ('SUPPLIER', 'company_name', NULL, '공급사명', '공급사 회사명', 'STRING', NULL, '[]', 'company_name', 1, TRUE, FALSE),
        ('SUPPLIER', 'code', NULL, '공급사 코드', '내부 식별용 공급사 코드', 'STRING', NULL, '[]', 'code', 2, FALSE, TRUE),
        ('SUPPLIER', 'country', NULL, '국가', '공급사 국가', 'STRING', NULL, '[]', 'country', 3, FALSE, TRUE),
        ('SUPPLIER', 'contact_info', NULL, '연락처', '공급사 연락처 정보', 'STRING', NULL, '[]', 'contact_info', 4, FALSE, TRUE),
        ('DRAWING', 'drawing_number', NULL, '도면번호', '도면의 식별 번호', 'STRING', NULL, '[]', 'drawing_number', 1, FALSE, FALSE),
        ('DRAWING', 'name', NULL, '도면명', '사람이 읽을 수 있는 도면 이름', 'STRING', NULL, '[]', 'name', 2, TRUE, FALSE),
        ('DRAWING', 'version', NULL, '버전', '도면 버전', 'STRING', NULL, '[]', 'version', 3, FALSE, TRUE),
        ('DRAWING', 'status', NULL, '상태', '도면 상태', 'STRING', NULL, '[]', 'status', 4, FALSE, TRUE),
        ('DRAWING', 'dimension', NULL, '규격', '도면 규격', 'STRING', NULL, '[]', 'dimension', 5, FALSE, TRUE),
        ('DRAWING', 'source_type', NULL, '원본 유형', '도면 원본 유형', 'STRING', NULL, '[]', 'source_type', 6, FALSE, TRUE),
        ('DRAWING', 'original_file_key', NULL, '원본 파일 키', '원본 파일 저장 키', 'STRING', NULL, '[]', 'original_file_key', 7, FALSE, TRUE),
        ('BOM_LINK', 'quantity', NULL, '수량', 'BOM 링크 수량', 'FLOAT', NULL, '[]', 'quantity', 1, TRUE, FALSE),
        ('PART_SUPPLIER', 'unit_cost', NULL, '단가', '부품-공급사 단가', 'FLOAT', NULL, '[]', 'unit_cost', 1, FALSE, TRUE)
) AS seed(
    owner_type,
    property_key,
    part_system_property_kind,
    display_name,
    description,
    value_type,
    option_mode,
    options_json,
    storage_binding,
    display_order,
    is_required,
    is_active_configurable
)
LEFT JOIN "system_property_overrides" overrides
    ON overrides.owner_type = seed.owner_type
    AND overrides.property_key = seed.property_key
LEFT JOIN "property_definitions" existing
    ON existing.owner_type = seed.owner_type
    AND existing.property_key = seed.property_key
WHERE existing.id IS NULL;

-- changeset codex:20260321170000-16 splitStatements:false
DROP TABLE "system_property_overrides";
