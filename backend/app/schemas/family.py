from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, EmailStr, Field


class FamilyGroupCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)


class FamilyGroupResponse(BaseModel):
    id: str
    name: str
    owner_user_id: str
    member_count: int
    created_at: str


class FamilyInvitationCreate(BaseModel):
    group_id: str
    invitee_email: EmailStr
    role: str = Field(default="FATHER", max_length=30)
    display_label: str = Field(default="Papa", max_length=80)
    father_friendly_mode: bool = True


class FamilyInvitationResponse(BaseModel):
    id: str
    group_id: str
    invitee_email: str
    role: str
    display_label: str
    father_friendly_mode: bool
    status: str
    invite_code: Optional[str] = None
    expires_at: str
    created_at: str


class FamilyInvitationAccept(BaseModel):
    invite_code: str = Field(..., min_length=6, max_length=120)


class FamilyMemberResponse(BaseModel):
    id: str
    group_id: str
    user_id: str
    full_name: str
    email: str
    role: str
    display_label: str
    father_friendly_mode: bool
    share_shared_reminders: bool
    share_family_routines: bool
    joined_at: str


class FamilyMemberUpdate(BaseModel):
    role: Optional[str] = Field(default=None, max_length=30)
    display_label: Optional[str] = Field(default=None, max_length=80)
    father_friendly_mode: Optional[bool] = None
    share_shared_reminders: Optional[bool] = None
    share_family_routines: Optional[bool] = None


class SharedReminderCreate(BaseModel):
    group_id: str
    title: str = Field(..., min_length=1, max_length=200)
    notes: str = Field(default="", max_length=1000)
    remind_at: datetime


class SharedReminderUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    notes: Optional[str] = Field(default=None, max_length=1000)
    remind_at: Optional[datetime] = None
    is_completed: Optional[bool] = None


class SharedReminderResponse(BaseModel):
    id: str
    group_id: str
    created_by_user_id: str
    title: str
    notes: str
    remind_at: str
    is_completed: bool
    created_at: str
    updated_at: str


class FamilyDashboardResponse(BaseModel):
    groups: List[FamilyGroupResponse]
    members: List[FamilyMemberResponse]
    pending_invitations: List[FamilyInvitationResponse]
    shared_reminders: List[SharedReminderResponse]
