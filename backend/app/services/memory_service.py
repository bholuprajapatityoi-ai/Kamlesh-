import re
from typing import Optional
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.repositories.memory_repository import MemoryRepository
from app.schemas.memory import (
    ALLOWED_MEMORY_CATEGORIES,
    MemoryAuditEventResponse,
    MemoryCreateRequest,
    MemoryListResponse,
    MemoryRecordResponse,
    MemoryUpdateRequest,
)

SENSITIVE_MEMORY_PATTERNS = [
    re.compile(r"\b(password|passcode|pin\s*code|otp|cvv|secret\s*key|jwt|bearer\s+token|api\s*key)\b", re.I),
    re.compile(r"(sk-[a-zA-Z0-9]{16,}|ghp_[a-zA-Z0-9]{20,}|AIza[0-9A-Za-z\-_]{25,})"),
]


class MemoryService:
    def __init__(self, db: Session) -> None:
        self.repo = MemoryRepository(db)

    def _validate_content(self, content: str) -> str:
        cleaned = content.strip()
        if len(cleaned) < 2:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Memory content cannot be empty.",
            )
        for pattern in SENSITIVE_MEMORY_PATTERNS:
            if pattern.search(cleaned):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Passwords, PINs, OTPs, and API tokens cannot be stored in personal memory records.",
                )
        return cleaned

    def _validate_category(self, category: str) -> str:
        c = category.strip()
        if c not in ALLOWED_MEMORY_CATEGORIES:
            return "Custom Notes"
        return c

    def _to_dto(self, rec) -> MemoryRecordResponse:
        return MemoryRecordResponse(
            id=rec.id,
            user_id=rec.user_id,
            category=rec.category,
            content=rec.content,
            expires_at=rec.expires_at.isoformat() if rec.expires_at else None,
            family_shared=rec.family_shared,
            created_at=rec.created_at.isoformat(),
            updated_at=rec.updated_at.isoformat(),
        )

    def create_memory(self, user_id: str, req: MemoryCreateRequest) -> MemoryRecordResponse:
        if not req.explicit_user_confirmed:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Explicit user confirmation is required before saving permanent personal memory.",
            )
        content = self._validate_content(req.content)
        category = self._validate_category(req.category)
        rec = self.repo.create(
            user_id=user_id,
            category=category,
            content=content,
            expires_at=req.expires_at,
            family_shared=req.family_shared,
        )
        return self._to_dto(rec)

    def list_memories(self, user_id: str, category: Optional[str] = None) -> MemoryListResponse:
        self.repo.ensure_default_categories()
        items = [self._to_dto(r) for r in self.repo.list_by_user(user_id, category)]
        return MemoryListResponse(
            total=len(items),
            categories=ALLOWED_MEMORY_CATEGORIES,
            items=items,
        )

    def get_memory(self, user_id: str, memory_id: str) -> MemoryRecordResponse:
        rec = self.repo.get_by_id_for_user(memory_id, user_id)
        if not rec:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Memory not found.")
        return self._to_dto(rec)

    def update_memory(
        self, user_id: str, memory_id: str, req: MemoryUpdateRequest
    ) -> MemoryRecordResponse:
        rec = self.repo.get_by_id_for_user(memory_id, user_id)
        if not rec:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Memory not found.")
        if req.category is not None:
            rec.category = self._validate_category(req.category)
        if req.content is not None:
            rec.content = self._validate_content(req.content)
        if req.expires_at is not None:
            rec.expires_at = req.expires_at
        if req.family_shared is not None:
            rec.family_shared = req.family_shared
        return self._to_dto(self.repo.update(rec))

    def delete_memory(self, user_id: str, memory_id: str) -> None:
        rec = self.repo.get_by_id_for_user(memory_id, user_id)
        if not rec:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Memory not found.")
        self.repo.delete_one(rec)

    def clear_all_memories(self, user_id: str) -> int:
        return self.repo.delete_all_for_user(user_id)

    def list_audit_events(self, user_id: str):
        return [
            MemoryAuditEventResponse(
                id=e.id,
                memory_id=e.memory_id,
                action=e.action,
                category=e.category,
                detail=e.detail,
                created_at=e.created_at.isoformat(),
            )
            for e in self.repo.list_audit_events(user_id)
        ]
