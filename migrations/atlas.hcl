variable "db_url" {
  type    = string
  default = "postgres://fabbit:fabbit@localhost:5432/fabbit?sslmode=disable"
}

data "external_schema" "public" {
  program = [
    "./gradlew",
    "-q",
    "schemaExportPublic"
  ]
}

data "external_schema" "tenant" {
  program = [
    "./gradlew",
    "-q",
    "schemaExportTenant"
  ]
}

// DB에서 tenant_* 스키마 목록을 동적 조회
data "sql" "tenants" {
  url   = var.db_url
  query = "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%'"
}

env "public" {
  src = data.external_schema.public.url
  dev = "docker://postgres/18/dev?search_path=public"
  url = "${var.db_url}&search_path=public"

  migration {
    dir = "file://migrations/public"
  }

  format {
    migrate {
      diff = "{{ sql . \"  \" }}"
    }
  }
}

// diff 생성용 (단일 환경)
env "tenant" {
  src = data.external_schema.tenant.url
  dev = "docker://postgres/18/dev?search_path=public"

  migration {
    dir = "file://migrations/tenant"
  }

  format {
    migrate {
      diff = "{{ sql . \"  \" }}"
    }
  }
}

// 모든 테넌트에 마이그레이션 적용용
env "tenant-apply" {
  for_each = toset(data.sql.tenants.values)
  url      = urlsetpath(var.db_url, each.value)

  migration {
    dir = "file://migrations/tenant"
  }
}
