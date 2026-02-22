from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # ── 앱 기본 ──
    app_name: str = "Fabbit Ontology Engine"
    debug: bool = False
    log_level: str = "INFO"
    base_domain: str = "lvh.me"  # lvh.me (local) / fabbit.io (prod)
    base_api_url: str = "http://localhost:8000"  # webhook callback URL 구성용

    # ── Background Worker ──
    background_max_workers: int = 3

    # ── PostgreSQL (Apache AGE) ──
    database_host: str = "localhost"
    database_port: int = 5432
    database_name: str = "fabbit"
    database_user: str = "fabbit"
    database_password: str = "fabbit"
    graph_name: str = "fabbit_graph"

    # ── JWT 인증 ──
    jwt_secret_key: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    jwt_issuer: str = "fabbit"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 7

    # ── S3 호환 스토리지 (Cloudflare R2 / MinIO) ──
    storage_endpoint: str = "http://localhost:9000"
    storage_access_key: str = "minioadmin"
    storage_secret_key: str = "minioadmin"
    storage_bucket: str = "fabbit"
    storage_public_url: str = ""

    # ── Drawing Converter MSA ──
    drawing_converter_url: str = ""  # 빈 문자열이면 비활성화
    drawing_converter_secret: str = ""

    # ── LLM (OpenRouter) ──
    llm_api_key: str = ""
    llm_base_url: str = "https://openrouter.ai/api/v1"

    # ── Cloudflare Turnstile (봇 방지) ──
    turnstile_secret_key: str = ""
    turnstile_enabled: bool = False

    # ── OpenTelemetry ──
    otel_enabled: bool = False
    otel_service_name: str = "fabbit-ontology-engine"
    otel_exporter_endpoint: str = ""
    otel_exporter_headers: str = ""

    @property
    def database_dsn(self) -> str:
        return (
            f"host={self.database_host} "
            f"port={self.database_port} "
            f"dbname={self.database_name} "
            f"user={self.database_user} "
            f"password={self.database_password}"
        )

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.database_user}:{self.database_password}"
            f"@{self.database_host}:{self.database_port}/{self.database_name}"
        )

    model_config = {"env_file": ".env", "extra": "ignore"}


settings = Settings()
