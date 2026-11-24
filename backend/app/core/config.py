from functools import lru_cache
from typing import List
from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    APP_NAME: str = "GOLU AI Backend"
    APP_VERSION: str = "1.1.0"
    ENVIRONMENT: str = "development"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000

    DATABASE_URL: str = Field(
        default="postgresql+psycopg://golu_user:golu_pass@localhost:5432/golu_db"
    )
    DB_POOL_SIZE: int = 10
    DB_MAX_OVERFLOW: int = 20
    DB_POOL_TIMEOUT: int = 30

    REDIS_URL: str = ""

    SECRET_KEY: str = Field(default="dev_only_app_secret_change_in_production_64_chars_minimum")
    JWT_SECRET_KEY: str = Field(default="dev_only_jwt_secret_change_in_production_64_chars_minimum")
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 14
    RESET_TOKEN_EXPIRE_MINUTES: int = 30
    EMAIL_VERIFICATION_EXPIRE_MINUTES: int = 60

    AI_PROVIDER: str = "openai"
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4o-mini"
    OPENAI_VISION_MODEL: str = "gpt-4o-mini"
    OPENAI_IMAGE_MODEL: str = "dall-e-3"
    OPENAI_TIMEOUT_SECONDS: float = 25.0
    TAVILY_API_KEY: str = ""

    SMTP_HOST: str = ""
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM_EMAIL: str = ""
    SMTP_USE_TLS: bool = True

    CORS_ALLOWED_ORIGINS: str = "*"
    LOGIN_RATE_LIMIT_PER_MINUTE: int = 10
    AI_RATE_LIMIT_PER_MINUTE: int = 30
    MAX_REQUEST_SIZE_BYTES: int = 1_048_576

    @model_validator(mode="after")
    def normalize_and_validate(self) -> "Settings":
        db_url = self.DATABASE_URL.strip()
        if db_url.startswith("postgres://"):
            db_url = "postgresql+psycopg://" + db_url[len("postgres://"):]
        elif db_url.startswith("postgresql://"):
            db_url = "postgresql+psycopg://" + db_url[len("postgresql://"):]
        self.DATABASE_URL = db_url

        if self.ENVIRONMENT.lower() == "production":
            insecure_defaults = (
                "dev_only",
                "CHANGE_ME",
                "secret",
            )
            if any(marker in self.JWT_SECRET_KEY for marker in insecure_defaults) or len(self.JWT_SECRET_KEY) < 32:
                raise ValueError(
                    "JWT_SECRET_KEY must be set to a strong 32+ character secret in production."
                )
            if any(marker in self.SECRET_KEY for marker in insecure_defaults) or len(self.SECRET_KEY) < 32:
                raise ValueError(
                    "SECRET_KEY must be set to a strong 32+ character secret in production."
                )
            if self.DATABASE_URL.startswith("sqlite"):
                raise ValueError("SQLite is not permitted in production; configure PostgreSQL DATABASE_URL.")
        return self

    @property
    def cors_origins(self) -> List[str]:
        raw = self.CORS_ALLOWED_ORIGINS.strip()
        if raw == "*":
            return ["*"]
        return [o.strip() for o in raw.split(",") if o.strip()]

    @property
    def email_delivery_configured(self) -> bool:
        return bool(self.SMTP_HOST.strip() and self.SMTP_FROM_EMAIL.strip())

    @property
    def openai_configured(self) -> bool:
        return bool(self.OPENAI_API_KEY.strip())

    @property
    def redis_configured(self) -> bool:
        return bool(self.REDIS_URL.strip())

    def missing_production_config(self) -> List[str]:
        missing = []
        if not self.openai_configured:
            missing.append("OPENAI_API_KEY")
        if not self.email_delivery_configured:
            missing.append("SMTP_HOST/SMTP_FROM_EMAIL")
        if "dev_only" in self.JWT_SECRET_KEY or "CHANGE_ME" in self.JWT_SECRET_KEY:
            missing.append("JWT_SECRET_KEY")
        return missing


@lru_cache
def get_settings() -> Settings:
    return Settings()
