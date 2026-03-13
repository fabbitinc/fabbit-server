-- liquibase formatted sql

-- changeset seongha.moon:1773372600000-1 splitStatements:false
CREATE TABLE "property_definitions" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "owner_type" VARCHAR(50) NOT NULL,
    "display_name" VARCHAR(200) NOT NULL,
    "description" TEXT,
    "value_type" VARCHAR(20) NOT NULL,
    "option_mode" VARCHAR(20),
    "options_json" JSONB NOT NULL DEFAULT '[]'::jsonb,
    "display_order" INTEGER NOT NULL,
    "is_required" BOOLEAN NOT NULL,
    "is_active" BOOLEAN NOT NULL,
    CONSTRAINT "property_definitions_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_property_definitions_owner_type_display_name" UNIQUE ("owner_type", "display_name"),
    CONSTRAINT "ck_property_definitions_display_order_non_negative" CHECK ("display_order" >= 0),
    CONSTRAINT "ck_property_definitions_options_json_array" CHECK (jsonb_typeof("options_json") = 'array'),
    CONSTRAINT "ck_property_definitions_option_mode" CHECK (
        (
            "value_type" = 'OPTION'
            AND "option_mode" IN ('FIXED', 'CREATABLE')
        )
        OR (
            "value_type" <> 'OPTION'
            AND "option_mode" IS NULL
        )
    )
);

-- changeset seongha.moon:1773372600000-2 splitStatements:false
CREATE INDEX "ix_property_definitions_owner_type_is_active_display_order"
ON "property_definitions" USING btree ("owner_type", "is_active", "display_order");

-- changeset seongha.moon:1773372600000-3 splitStatements:false
CREATE TABLE "system_property_overrides" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "owner_type" VARCHAR(50) NOT NULL,
    "property_key" VARCHAR(100) NOT NULL,
    "display_name_override" VARCHAR(200),
    "display_order" INTEGER,
    "is_active" BOOLEAN NOT NULL,
    CONSTRAINT "system_property_overrides_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_system_property_overrides_owner_type_property_key" UNIQUE ("owner_type", "property_key"),
    CONSTRAINT "ck_system_property_overrides_display_order_non_negative" CHECK (
        "display_order" IS NULL OR "display_order" >= 0
    )
);

-- changeset seongha.moon:1773372600000-4 splitStatements:false
CREATE INDEX "ix_system_property_overrides_owner_type_is_active_display_order"
ON "system_property_overrides" USING btree ("owner_type", "is_active", "display_order");
