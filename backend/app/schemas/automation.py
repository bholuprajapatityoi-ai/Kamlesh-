from typing import List, Optional
from pydantic import BaseModel, Field

ALLOWED_TRIGGER_TYPES = {"TIME", "DAY", "REMINDER", "COMMAND", "ANDROID_EVENT"}
ALLOWED_CONDITION_TYPES = {"TIME_WINDOW", "DAY_OF_WEEK", "PREFERENCE", "DEVICE_STATE"}
ALLOWED_ACTION_TYPES = {
    "SPEAK_MESSAGE",
    "SHOW_NOTIFICATION",
    "OPEN_APP",
    "CREATE_REMINDER",
    "READ_TIMETABLE",
    "ANDROID_ACTION",
}


class AutomationConditionDto(BaseModel):
    condition_type: str = Field(..., max_length=40)
    condition_value: str = Field(..., max_length=160)


class AutomationActionDto(BaseModel):
    action_type: str = Field(..., max_length=40)
    action_payload: str = Field(default="", max_length=1000)


class AutomationCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)
    trigger_type: str = Field(..., max_length=40)
    trigger_value: str = Field(..., min_length=1, max_length=160)
    is_enabled: bool = True
    requires_confirmation: bool = False
    confirmed_sensitive: bool = False
    conditions: List[AutomationConditionDto] = []
    actions: List[AutomationActionDto] = []


class AutomationUpdate(BaseModel):
    name: Optional[str] = Field(default=None, min_length=2, max_length=120)
    trigger_type: Optional[str] = Field(default=None, max_length=40)
    trigger_value: Optional[str] = Field(default=None, min_length=1, max_length=160)
    is_enabled: Optional[bool] = None
    requires_confirmation: Optional[bool] = None
    conditions: Optional[List[AutomationConditionDto]] = None
    actions: Optional[List[AutomationActionDto]] = None


class AutomationResponse(BaseModel):
    id: str
    name: str
    trigger_type: str
    trigger_value: str
    is_enabled: bool
    requires_confirmation: bool
    conditions: List[AutomationConditionDto]
    actions: List[AutomationActionDto]
    created_at: str
    updated_at: str


class AutomationExecutionLogRequest(BaseModel):
    status: str = Field(default="EXECUTED", max_length=30)
    summary: str = Field(..., min_length=1, max_length=255)


class AutomationExecutionLogResponse(BaseModel):
    id: str
    rule_id: str
    status: str
    summary: str
    executed_at: str
