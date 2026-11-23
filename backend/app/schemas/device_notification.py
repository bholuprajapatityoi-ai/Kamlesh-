from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class DeviceRegisterRequest(BaseModel):
    device_identifier: str = Field(min_length=2, max_length=120)
    device_name: str = Field(default="Android Device", min_length=1, max_length=120)
    platform: str = Field(default="android", min_length=2, max_length=30)
    os_version: str = Field(default="", max_length=40)
    app_version: str = Field(default="1.1.0", max_length=40)
    push_token: str = Field(default="", max_length=512)


class DeviceResponse(BaseModel):
    id: str
    device_identifier: str
    device_name: str
    platform: str
    os_version: str
    app_version: str
    push_token_masked: str
    is_active: bool
    last_seen_at: str
    created_at: str


class NotificationCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    body: str = Field(default="", max_length=2000)
    category: str = Field(default="REMINDER", max_length=40)
    channel: str = Field(default="IN_APP", max_length=40)
    scheduled_for: Optional[datetime] = None


class NotificationResponse(BaseModel):
    id: str
    title: str
    body: str
    category: str
    channel: str
    status: str
    retry_count: int
    scheduled_for: Optional[str] = None
    delivered_at: Optional[str] = None
    created_at: str
