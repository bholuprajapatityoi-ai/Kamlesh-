from typing import Optional
from pydantic import BaseModel, Field


class PhonePreferencesResponse(BaseModel):
    confirm_before_call: bool
    allow_volume_control: bool
    allow_media_control: bool
    allow_contact_lookup: bool
    updated_at: str


class PhonePreferencesUpdate(BaseModel):
    confirm_before_call: Optional[bool] = None
    allow_volume_control: Optional[bool] = None
    allow_media_control: Optional[bool] = None
    allow_contact_lookup: Optional[bool] = None


class ContactAliasCreate(BaseModel):
    nickname: str = Field(..., min_length=1, max_length=60)
    contact_name: str = Field(..., min_length=1, max_length=120)
    phone_hint: str = Field(default="", max_length=40)


class ContactAliasUpdate(BaseModel):
    nickname: Optional[str] = Field(default=None, min_length=1, max_length=60)
    contact_name: Optional[str] = Field(default=None, min_length=1, max_length=120)
    phone_hint: Optional[str] = Field(default=None, max_length=40)


class ContactAliasResponse(BaseModel):
    id: str
    nickname: str
    contact_name: str
    phone_hint: str
    created_at: str
    updated_at: str


class AppPreferenceUpsert(BaseModel):
    alias_name: str = Field(..., min_length=1, max_length=80)
    package_name: str = Field(..., min_length=2, max_length=180)
    is_favourite: bool = True


class AppPreferenceResponse(BaseModel):
    id: str
    alias_name: str
    package_name: str
    is_favourite: bool
    updated_at: str
