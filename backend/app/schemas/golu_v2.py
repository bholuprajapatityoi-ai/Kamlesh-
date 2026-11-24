from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


# Feature 1: Smart Screen Understanding
class ScreenUnderstandRequest(BaseModel):
    screen_text: str = Field(min_length=1, max_length=12000)
    action: str = Field(default="EXPLAIN", pattern="^(EXPLAIN|SUMMARIZE|EXTRACT|QA)$")
    question: str = Field(default="", max_length=600)
    app_context: str = Field(default="", max_length=120)
    language: str = Field(default="Hinglish", max_length=30)
    consent_granted: bool = False


class ScreenUnderstandResponse(BaseModel):
    action: str
    answer: str
    extracted_highlights: List[str] = Field(default_factory=list)
    provider: str
    ai_backed: bool


# Feature 2: Live Camera AI
class CameraAnalyzeRequest(BaseModel):
    image_base64: str = Field(default="", max_length=4_500_000)
    question: str = Field(default="Describe what you see and identify key objects or text.", max_length=500)
    mode: str = Field(default="SCENE", pattern="^(SCENE|OBJECT|OCR|VQA|TRANSLATE)$")
    language: str = Field(default="Hinglish", max_length=30)
    consent_granted: bool = False


class CameraAnalyzeResponse(BaseModel):
    mode: str
    description: str
    detected_text: str = ""
    objects: List[str] = Field(default_factory=list)
    provider: str
    ai_backed: bool


# Feature 3: Document Intelligence
class DocumentUploadRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    mime_type: str = Field(default="text/plain", max_length=80)
    text_content: str = Field(default="", max_length=50000)
    file_base64: str = Field(default="", max_length=4_500_000)
    consent_granted: bool = False


class DocumentResponse(BaseModel):
    id: str
    title: str
    mime_type: str
    file_size_bytes: int
    summary: str
    key_points: List[str] = Field(default_factory=list)
    preview_text: str = ""
    created_at: datetime


class DocumentQueryRequest(BaseModel):
    question: str = Field(min_length=1, max_length=600)
    language: str = Field(default="Hinglish", max_length=30)


class DocumentQueryResponse(BaseModel):
    document_id: str
    answer: str
    matched_excerpts: List[str] = Field(default_factory=list)
    ai_backed: bool


# Feature 4: Smart Notification Intelligence
class NotificationIngestRequest(BaseModel):
    source_app: str = Field(default="Android", max_length=120)
    title: str = Field(min_length=1, max_length=200)
    body_preview: str = Field(default="", max_length=2000)
    consent_granted: bool = False
    create_reminder_if_important: bool = False


class NotificationInsightResponse(BaseModel):
    id: str
    source_app: str
    title: str
    body_preview: str
    category: str
    priority_score: int
    is_important: bool
    ai_summary: str
    converted_reminder_id: Optional[str] = None
    created_at: datetime


class NotificationSummaryResponse(BaseModel):
    total_count: int
    important_count: int
    summary: str
    items: List[NotificationInsightResponse] = Field(default_factory=list)


# Feature 5: Personal Knowledge Brain (unified with MemoryRecord)
class KnowledgeNoteRequest(BaseModel):
    title: str = Field(min_length=1, max_length=160)
    category: str = Field(default="Custom Notes", max_length=60)
    content: str = Field(min_length=1, max_length=6000)
    tags: List[str] = Field(default_factory=list)
    family_shared: bool = False


class KnowledgeNoteResponse(BaseModel):
    id: str
    title: str
    category: str
    content: str
    tags: List[str] = Field(default_factory=list)
    family_shared: bool
    created_at: datetime
    updated_at: datetime


# Feature 6: Live Language Translator
class TranslateRequest(BaseModel):
    text: str = Field(min_length=1, max_length=4000)
    source_language: str = Field(default="auto", max_length=40)
    target_language: str = Field(default="Hindi", max_length=40)
    mode: str = Field(default="TEXT", pattern="^(TEXT|SPEECH|CONVERSATION)$")


class TranslationResponse(BaseModel):
    id: str
    source_language: str
    target_language: str
    source_text: str
    translated_text: str
    mode: str
    ai_backed: bool
    created_at: datetime


# Feature 7: Deep Research Mode
class ResearchRequest(BaseModel):
    query: str = Field(min_length=2, max_length=300)
    language: str = Field(default="English", max_length=30)


class CitationItem(BaseModel):
    title: str
    url: str
    snippet: str


class ResearchReportResponse(BaseModel):
    id: str
    query: str
    summary: str
    verified_findings: str
    uncertain_points: str
    citations: List[CitationItem] = Field(default_factory=list)
    provider_used: str
    created_at: datetime


# Feature 8: Advanced Offline Brain
class OfflineManifestResponse(BaseModel):
    offline_capable_features: List[str]
    online_required_features: List[str]
    local_command_examples: List[str]
    cloud_ai_note: str


# Feature 9: Life Planner AI
class PlannerSuggestRequest(BaseModel):
    goal_focus: str = Field(default="Productive day & study balance", max_length=300)
    available_hours: int = Field(default=6, ge=1, le=16)
    language: str = Field(default="Hinglish", max_length=30)


class PlannedItemProposal(BaseModel):
    title: str
    item_type: str = "TASK"
    priority: str = "MEDIUM"
    suggested_slot: str = ""
    reason: str = ""


class PlannerSuggestResponse(BaseModel):
    summary: str
    proposals: List[PlannedItemProposal] = Field(default_factory=list)
    requires_user_approval: bool = True


class PlannerApproveRequest(BaseModel):
    items: List[PlannedItemProposal] = Field(min_length=1, max_length=15)


# Feature 10: Coding Assistant
class CodingAssistRequest(BaseModel):
    prompt: str = Field(min_length=2, max_length=6000)
    language: str = Field(default="kotlin", max_length=40)
    task_type: str = Field(default="EXPLAIN", pattern="^(EXPLAIN|DEBUG|GENERATE|REVIEW)$")


class CodingAssistResponse(BaseModel):
    id: str
    language: str
    task_type: str
    prompt: str
    code_output: str
    explanation: str
    security_note: str
    ai_backed: bool
    created_at: datetime


# Feature 11: Creative Studio
class CreativeRequest(BaseModel):
    asset_type: str = Field(default="IMAGE", pattern="^(IMAGE|POSTER_IDEA|DESIGN_CONCEPT)$")
    prompt: str = Field(min_length=3, max_length=1500)
    style: str = Field(default="Futuristic Navy & Cyan", max_length=80)


class CreativeAssetResponse(BaseModel):
    id: str
    asset_type: str
    prompt: str
    revised_prompt: str = ""
    image_url: str = ""
    design_notes: str = ""
    status: str
    error_detail: str = ""
    created_at: datetime


# Feature 12: Voice Recorder Intelligence
class RecordingCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=160)
    duration_seconds: int = Field(default=0, ge=0, le=36000)
    transcript: str = Field(default="", max_length=50000)
    local_file_hint: str = Field(default="", max_length=255)
    generate_summary: bool = True


class RecordingUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, max_length=160)
    transcript: Optional[str] = Field(default=None, max_length=50000)
    regenerate_summary: bool = False


class RecordingResponse(BaseModel):
    id: str
    title: str
    duration_seconds: int
    transcript: str
    summary: str
    lecture_notes: str
    local_file_hint: str
    created_at: datetime
    updated_at: datetime


# Feature 13: Smart Home Control
class SmartHomeConfigRequest(BaseModel):
    provider_type: str = Field(default="HOME_ASSISTANT", max_length=40)
    hub_url: str = Field(default="", max_length=255)
    access_token: str = Field(default="", max_length=512)
    authorize: bool = False


class SmartHomeConfigResponse(BaseModel):
    provider_type: str
    hub_url: str
    token_hint: str
    is_authorized: bool
    connection_state: str
    detail: str


class SmartHomeDeviceCreateRequest(BaseModel):
    external_entity_id: str = Field(min_length=1, max_length=120)
    name: str = Field(min_length=1, max_length=120)
    room: str = Field(default="Living Room", max_length=80)
    device_type: str = Field(default="LIGHT", max_length=40)


class SmartHomeDeviceCommandRequest(BaseModel):
    turn_on: bool
    brightness_pct: int = Field(default=100, ge=0, le=100)
    confirmed_sensitive: bool = False


class SmartHomeDeviceResponse(BaseModel):
    id: str
    external_entity_id: str
    name: str
    room: str
    device_type: str
    is_on: bool
    brightness_pct: int
    status: str
    last_synced_at: Optional[datetime] = None


# Feature 14: Expense Tracker
class ExpenseCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=160)
    category: str = Field(default="General", max_length=50)
    amount: float = Field(gt=0.0, le=100_000_000.0)
    currency: str = Field(default="INR", max_length=10)
    notes: str = Field(default="", max_length=255)


class BudgetUpdateRequest(BaseModel):
    month_key: str = Field(pattern="^\\d{4}-\\d{2}$")
    limit_amount: float = Field(ge=0.0, le=100_000_000.0)
    currency: str = Field(default="INR", max_length=10)


class ExpenseRecordResponse(BaseModel):
    id: str
    title: str
    category: str
    amount: float
    currency: str
    notes: str
    spent_at: datetime


class ExpenseSummaryResponse(BaseModel):
    month_key: str
    budget_limit: float
    total_spent: float
    remaining_budget: float
    currency: str
    by_category: Dict[str, float] = Field(default_factory=dict)
    items: List[ExpenseRecordResponse] = Field(default_factory=list)


# Feature 15: Custom AI Personality
class PersonalityUpdateRequest(BaseModel):
    response_style: Optional[str] = Field(default=None, max_length=40)
    humor_level: Optional[int] = Field(default=None, ge=0, le=100)
    response_length: Optional[str] = Field(default=None, pattern="^(SHORT|MEDIUM|DETAILED)$")
    greeting_message: Optional[str] = Field(default=None, max_length=255)
    voice_gender: Optional[str] = Field(default=None, max_length=20)
    avatar_style: Optional[str] = Field(default=None, max_length=40)
    custom_system_notes: Optional[str] = Field(default=None, max_length=255)


class PersonalityResponse(BaseModel):
    response_style: str
    humor_level: int
    response_length: str
    greeting_message: str
    voice_gender: str
    avatar_style: str
    custom_system_notes: str


# Feature 16: Smart App Intelligence
class AppShortcutUpsertRequest(BaseModel):
    alias_name: str = Field(min_length=1, max_length=80)
    package_name: str = Field(min_length=2, max_length=180)
    is_favourite: bool = True
    shortcut_command: str = Field(default="", max_length=120)
    increment_launch: bool = False


class SmartAppPreferenceResponse(BaseModel):
    id: str
    alias_name: str
    package_name: str
    is_favourite: bool
    launch_count: int
    shortcut_command: str
    last_launched_at: Optional[datetime] = None


# Feature 17: Study Tutor Mode
class StudyAskRequest(BaseModel):
    subject: str = Field(min_length=1, max_length=100)
    topic: str = Field(min_length=1, max_length=180)
    language: str = Field(default="English", max_length=30)
    difficulty: str = Field(default="Intermediate", max_length=30)


class StudyProgressUpdateRequest(BaseModel):
    mastery_pct: int = Field(ge=0, le=100)


class StudySessionResponse(BaseModel):
    id: str
    subject: str
    topic: str
    language: str
    difficulty: str
    explanation: str
    practice_questions: List[str] = Field(default_factory=list)
    mastery_pct: int
    created_at: datetime


# Feature 18: Travel Intelligence
class TravelPlanRequest(BaseModel):
    origin: str = Field(default="", max_length=160)
    destination: str = Field(min_length=2, max_length=160)
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    interests: str = Field(default="food, landmarks, transit", max_length=120)


class TravelPlaceItem(BaseModel):
    name: str
    category: str
    address: str = ""
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class TravelPlanResponse(BaseModel):
    id: str
    origin: str
    destination: str
    route_summary: str
    nearby_places: List[TravelPlaceItem] = Field(default_factory=list)
    itinerary_notes: str
    live_data_notice: str
    created_at: datetime


# Feature 19: Multi-Device Sync
class SyncPushRequest(BaseModel):
    device_identifier: str = Field(min_length=3, max_length=120)
    scope: str = Field(default="ALL", max_length=40)
    conflict_policy: str = Field(default="SERVER_WINS", pattern="^(SERVER_WINS|CLIENT_WINS|LATEST_TIMESTAMP)$")
    client_updated_at: Optional[datetime] = None


class SyncCheckpointResponse(BaseModel):
    id: str
    device_identifier: str
    scope: str
    conflict_policy: str
    synced_items_count: int
    synced_at: datetime


class MultiDeviceOverviewResponse(BaseModel):
    devices: List[Dict[str, Any]]
    checkpoints: List[SyncCheckpointResponse]
    notes_count: int
    memories_count: int
    expenses_count: int


# Feature 20: Self-Diagnostic System
class DiagnosticCheckInput(BaseModel):
    mic_permission: bool = False
    camera_permission: bool = False
    location_permission: bool = False
    notification_listener_enabled: bool = False
    device_online: bool = False


class DiagnosticReportResponse(BaseModel):
    backend_ok: bool
    database_ok: bool
    redis_ok: bool
    openai_configured: bool
    search_api_configured: bool
    overall_status: str
    troubleshooting_suggestions: List[str] = Field(default_factory=list)
    recent_events: List[Dict[str, Any]] = Field(default_factory=list)
