from typing import List, Optional
from pydantic import BaseModel, Field


class AiChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    language: str = "Hinglish"
    response_style: str = "Playful Boss Mode"
    session_id: Optional[str] = None


class AiChatResponse(BaseModel):
    reply: str
    route: str
    intent: str
    session_id: str
    requires_confirmation: bool = False
    provider: str = "offline"
    model: Optional[str] = None
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class AiIntentRequest(BaseModel):
    text: str = Field(min_length=1, max_length=1000)


class AiIntentResponse(BaseModel):
    intent: str
    recommended_route: str
    confidence: float


class AiStatusResponse(BaseModel):
    online_ai_configured: bool
    provider: str = "openai"
    model: str
    offline_engine_ready: bool


class ClearContextRequest(BaseModel):
    session_id: Optional[str] = None


class ConversationMessageDto(BaseModel):
    id: str
    role: str
    content: str
    intent: str
    route: str
    created_at: str


class ConversationSessionDto(BaseModel):
    id: str
    title: str
    created_at: str
    updated_at: str
    messages: List[ConversationMessageDto] = []
