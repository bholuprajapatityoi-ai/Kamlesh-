from typing import Optional
from pydantic import BaseModel, Field


class MessagingPreferencesResponse(BaseModel):
    default_language: str
    always_confirm_before_open: bool
    signature_text: str
    updated_at: str


class MessagingPreferencesUpdate(BaseModel):
    default_language: Optional[str] = Field(default=None, min_length=2, max_length=30)
    always_confirm_before_open: Optional[bool] = None
    signature_text: Optional[str] = Field(default=None, max_length=120)


class MessagingTemplateCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=100)
    language: str = Field(default="Hinglish", min_length=2, max_length=30)
    body: str = Field(..., min_length=1, max_length=1000)


class MessagingTemplateUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=100)
    language: Optional[str] = Field(default=None, min_length=2, max_length=30)
    body: Optional[str] = Field(default=None, min_length=1, max_length=1000)


class MessagingTemplateResponse(BaseModel):
    id: str
    title: str
    language: str
    body: str
    created_at: str
    updated_at: str
