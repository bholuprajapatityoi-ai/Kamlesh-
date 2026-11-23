from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field

ALLOWED_MEMORY_CATEGORIES = [
    "Preferences",
    "Favourite Apps",
    "Contact Nicknames",
    "Timetable",
    "Daily Routines",
    "Reminders",
    "Custom Notes",
]


class MemoryCreateRequest(BaseModel):
    category: str = Field(default="Custom Notes", min_length=2, max_length=60)
    content: str = Field(..., min_length=2, max_length=2000)
    expires_at: Optional[datetime] = None
    family_shared: bool = False
    explicit_user_confirmed: bool = True


class MemoryUpdateRequest(BaseModel):
    category: Optional[str] = Field(default=None, min_length=2, max_length=60)
    content: Optional[str] = Field(default=None, min_length=2, max_length=2000)
    expires_at: Optional[datetime] = None
    family_shared: Optional[bool] = None


class MemoryRecordResponse(BaseModel):
    id: str
    user_id: str
    category: str
    content: str
    expires_at: Optional[str] = None
    family_shared: bool
    created_at: str
    updated_at: str


class MemoryListResponse(BaseModel):
    total: int
    categories: List[str]
    items: List[MemoryRecordResponse]


class MemoryAuditEventResponse(BaseModel):
    id: str
    memory_id: Optional[str] = None
    action: str
    category: str
    detail: str
    created_at: str
