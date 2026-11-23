from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class ReminderCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    notes: str = Field(default="", max_length=1000)
    remind_at: datetime
    timezone_name: str = Field(default="Asia/Kolkata", max_length=64)
    recurrence: str = Field(default="NONE", max_length=30)
    notify_enabled: bool = True


class ReminderUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    notes: Optional[str] = Field(default=None, max_length=1000)
    remind_at: Optional[datetime] = None
    timezone_name: Optional[str] = Field(default=None, max_length=64)
    recurrence: Optional[str] = Field(default=None, max_length=30)
    is_completed: Optional[bool] = None
    notify_enabled: Optional[bool] = None


class ReminderResponse(BaseModel):
    id: str
    title: str
    notes: str
    remind_at: str
    timezone_name: str
    recurrence: str
    is_completed: bool
    notify_enabled: bool
    created_at: str
    updated_at: str


class TaskCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    priority: str = Field(default="MEDIUM", max_length=20)
    due_date: Optional[datetime] = None


class TaskUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    priority: Optional[str] = Field(default=None, max_length=20)
    due_date: Optional[datetime] = None
    is_completed: Optional[bool] = None


class TaskResponse(BaseModel):
    id: str
    title: str
    priority: str
    due_date: Optional[str] = None
    is_completed: bool
    created_at: str
    updated_at: str


class TimetableCreate(BaseModel):
    day_of_week: str = Field(..., min_length=2, max_length=20)
    start_time: str = Field(..., min_length=3, max_length=16)
    end_time: str = Field(..., min_length=3, max_length=16)
    subject_or_activity: str = Field(..., min_length=1, max_length=160)
    location: str = Field(default="", max_length=120)


class TimetableUpdate(BaseModel):
    day_of_week: Optional[str] = Field(default=None, min_length=2, max_length=20)
    start_time: Optional[str] = Field(default=None, min_length=3, max_length=16)
    end_time: Optional[str] = Field(default=None, min_length=3, max_length=16)
    subject_or_activity: Optional[str] = Field(default=None, min_length=1, max_length=160)
    location: Optional[str] = Field(default=None, max_length=120)


class TimetableResponse(BaseModel):
    id: str
    day_of_week: str
    start_time: str
    end_time: str
    subject_or_activity: str
    location: str
    created_at: str
    updated_at: str


class CalendarEventCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    description: str = Field(default="", max_length=1000)
    start_at: datetime
    end_at: datetime
    timezone_name: str = Field(default="Asia/Kolkata", max_length=64)


class CalendarEventUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    description: Optional[str] = Field(default=None, max_length=1000)
    start_at: Optional[datetime] = None
    end_at: Optional[datetime] = None
    timezone_name: Optional[str] = Field(default=None, max_length=64)


class CalendarEventResponse(BaseModel):
    id: str
    title: str
    description: str
    start_at: str
    end_at: str
    timezone_name: str
    created_at: str
    updated_at: str


class DailyRoutineCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=120)
    trigger_time: str = Field(..., min_length=3, max_length=16)
    summary_prompt: str = Field(default="", max_length=255)
    is_enabled: bool = True


class DailyRoutineResponse(BaseModel):
    id: str
    name: str
    trigger_time: str
    summary_prompt: str
    is_enabled: bool
    created_at: str
    updated_at: str


class NotificationPreferencesResponse(BaseModel):
    morning_greeting_enabled: bool
    evening_summary_enabled: bool
    reminder_notifications_enabled: bool
    battery_alert_threshold: int
    updated_at: str


class NotificationPreferencesUpdate(BaseModel):
    morning_greeting_enabled: Optional[bool] = None
    evening_summary_enabled: Optional[bool] = None
    reminder_notifications_enabled: Optional[bool] = None
    battery_alert_threshold: Optional[int] = Field(default=None, ge=5, le=50)
