from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # PostgreSQL (Apache AGE)
    database_host: str = "localhost"
    database_port: int = 5432
    database_name: str = "fabbit"
    database_user: str = "fabbit"
    database_password: str = "fabbit"

    # OpenAI
    openai_api_key: str = ""

    # Apache AGE 그래프 이름
    graph_name: str = "fabbit_graph"

    @property
    def database_dsn(self) -> str:
        return (
            f"host={self.database_host} "
            f"port={self.database_port} "
            f"dbname={self.database_name} "
            f"user={self.database_user} "
            f"password={self.database_password}"
        )

    model_config = {"env_file": ".env", "extra": "ignore"}


settings = Settings()
