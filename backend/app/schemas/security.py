from typing import List, Optional
from pydantic import BaseModel


class ActiveSessionResponse(BaseModel):
    id: str
    is_current: bool
    revoked: bool
    expires_at: str
    created_at: str


class SecurityEventResponse(BaseModel):
    id: str
    event_type: str
    severity: str
    detail: str
    ip_address: str
    created_at: str


class UserSecuritySettingsResponse(BaseModel):
    pin_lock_enabled: bool
    confirm_sensitive_actions: bool
    microphone_allowed: bool
    wake_word_monitoring_allowed: bool
    updated_at: str


class UserSecuritySettingsUpdate(BaseModel):
    pin_lock_enabled: Optional[bool] = None
    confirm_sensitive_actions: Optional[bool] = None
    microphone_allowed: Optional[bool] = None
    wake_word_monitoring_allowed: Optional[bool] = None


class SecurityDashboardResponse(BaseModel):
    settings: UserSecuritySettingsResponse
    active_sessions: List[ActiveSessionResponse]
    recent_events: List[SecurityEventResponse]
