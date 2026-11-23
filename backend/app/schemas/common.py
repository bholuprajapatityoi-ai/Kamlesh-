from typing import List, Optional
from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    backend_ok: bool
    database_ok: bool
    database_error: Optional[str] = None
    redis_ok: bool = False
    missing_config: List[str] = []
    timestamp: str


class DatabaseHealthResponse(BaseModel):
    status: str
    database_ok: bool
    database_engine: str
    latency_ms: float
    redis_ok: bool
    database_error: Optional[str] = None
    timestamp: str


class SystemStatusResponse(BaseModel):
    app: str
    version: str
    environment: str
    database_status: str
    redis_status: str = "unconfigured"
    openai_configured: bool
    email_delivery_configured: bool
    missing_config: List[str] = []


class SystemConfigResponse(BaseModel):
    api_version: str
    supported_languages: List[str]
    wake_phrase: str
    email_delivery_configured: bool
    online_ai_available: bool


class MessageResponse(BaseModel):
    message: str
    email_sent: Optional[bool] = None
    delivery_configured: Optional[bool] = None
