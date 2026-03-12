-- liquibase formatted sql

-- changeset seongha.moon:1773372600000-1 splitStatements:false
CREATE TABLE "property_definitions" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "owner_type" VARCHAR(50) NOT NULL,
    "property_key" VARCHAR(100),
    "display_name" VARCHAR(200) NOT NULL,
    "description" TEXT,
    "value_type" VARCHAR(20) NOT NULL,
    "options_json" JSONB NOT NULL DEFAULT '[]'::jsonb,
    "column_name" VARCHAR(100),
    "display_order" INTEGER NOT NULL,
    "is_system" BOOLEAN NOT NULL,
    "is_required" BOOLEAN NOT NULL,
    "is_active" BOOLEAN NOT NULL,
    CONSTRAINT "property_definitions_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_property_definitions_owner_type_property_key" UNIQUE ("owner_type", "property_key"),
    CONSTRAINT "uq_property_definitions_owner_type_display_name" UNIQUE ("owner_type", "display_name"),
    CONSTRAINT "uq_property_definitions_owner_type_column_name" UNIQUE ("owner_type", "column_name"),
    CONSTRAINT "ck_property_definitions_display_order_non_negative" CHECK ("display_order" >= 0),
    CONSTRAINT "ck_property_definitions_options_json_array" CHECK (jsonb_typeof("options_json") = 'array'),
    CONSTRAINT "ck_property_definitions_system_fields" CHECK (
        (
            "is_system" = TRUE
            AND "property_key" IS NOT NULL
            AND btrim("property_key") <> ''
            AND "column_name" IS NOT NULL
            AND btrim("column_name") <> ''
        )
        OR (
            "is_system" = FALSE
            AND "column_name" IS NULL
        )
    )
);

-- changeset seongha.moon:1773372600000-2 splitStatements:false
CREATE INDEX "ix_property_definitions_owner_type_is_active_display_order"
ON "property_definitions" USING btree ("owner_type", "is_active", "display_order");

-- changeset seongha.moon:1773372600000-3 splitStatements:false
INSERT INTO "property_definitions" (
    "id",
    "created_at",
    "updated_at",
    "owner_type",
    "property_key",
    "display_name",
    "description",
    "value_type",
    "options_json",
    "column_name",
    "display_order",
    "is_system",
    "is_required",
    "is_active"
)
VALUES
    ('7d07265f-a5a6-4a2c-9b74-17f432964d01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'part_number', '품번', '부품의 고유 식별자', 'STRING', '[]'::jsonb, 'part_number', 10, TRUE, TRUE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d02', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'name', '품명', '사람이 읽을 수 있는 부품 이름', 'STRING', '[]'::jsonb, 'name', 20, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d03', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'revision', '리비전', '부품 리비전', 'STRING', '[]'::jsonb, 'revision', 30, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d04', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'material', '재질', '부품 재질', 'STRING', '[]'::jsonb, 'material', 40, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d05', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'unit', '단위', '부품 수량 단위', 'STRING', '[]'::jsonb, 'unit', 50, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d06', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'description', '설명', '부품 설명', 'STRING', '[]'::jsonb, 'description', 60, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d07', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'category', '카테고리', '부품 분류', 'STRING', '[]'::jsonb, 'category', 70, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d08', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'is_phantom', '팬텀 여부', '팬텀 조립품 여부', 'BOOLEAN', '[]'::jsonb, 'is_phantom', 80, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d09', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'lifecycle_state', '수명주기 상태', '부품 수명주기 상태', 'STRING', '[]'::jsonb, 'lifecycle_state', 90, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART', 'lead_time_days', '리드타임(일)', '부품 리드타임(일)', 'INTEGER', '[]'::jsonb, 'lead_time_days', 100, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d11', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUPPLIER', 'company_name', '공급사명', '공급사 회사명', 'STRING', '[]'::jsonb, 'company_name', 10, TRUE, TRUE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUPPLIER', 'code', '공급사 코드', '내부 식별용 공급사 코드', 'STRING', '[]'::jsonb, 'code', 20, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d13', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUPPLIER', 'country', '국가', '공급사 국가', 'STRING', '[]'::jsonb, 'country', 30, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d14', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUPPLIER', 'contact_info', '연락처', '공급사 연락처 정보', 'STRING', '[]'::jsonb, 'contact_info', 40, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'drawing_number', '도면번호', '도면의 식별 번호', 'STRING', '[]'::jsonb, 'drawing_number', 10, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d16', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'name', '도면명', '사람이 읽을 수 있는 도면 이름', 'STRING', '[]'::jsonb, 'name', 20, TRUE, TRUE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'version', '버전', '도면 버전', 'STRING', '[]'::jsonb, 'version', 30, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'status', '상태', '도면 상태', 'STRING', '[]'::jsonb, 'status', 40, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d19', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'conversion_status', '변환 상태', '도면 변환 상태', 'STRING', '[]'::jsonb, 'conversion_status', 50, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'source_type', '소스 타입', '도면 소스 타입', 'STRING', '[]'::jsonb, 'source_type', 60, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d21', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DRAWING', 'dimension', '차원', '도면 차원 정보', 'STRING', '[]'::jsonb, 'dimension', 70, TRUE, FALSE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d22', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'BOM_LINK', 'quantity', '수량', 'BOM 관계 수량', 'INTEGER', '[]'::jsonb, 'quantity', 10, TRUE, TRUE, TRUE),
    ('7d07265f-a5a6-4a2c-9b74-17f432964d23', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PART_SUPPLIER', 'unit_cost', '단가', '공급사별 부품 단가', 'FLOAT', '[]'::jsonb, 'unit_cost', 10, TRUE, FALSE, TRUE);
