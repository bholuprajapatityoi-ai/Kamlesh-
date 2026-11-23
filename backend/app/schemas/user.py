from typing import Optional
from pydantic import BaseModel, Field


class UserProfileUpdateRequest(BaseModel):
    full_name: Optional[str] = Field(default=None, min_length=2, max_length=120)
    preferred_title: Optional[str] = Field(default=None, min_length=2, max_length=40)
    preferred_language: Optional[str] = Field(default=None, pattern="^(Hinglish|Hindi|English)$")
