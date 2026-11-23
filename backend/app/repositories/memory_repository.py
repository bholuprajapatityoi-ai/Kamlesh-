from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.memory import MemoryAuditEvent, MemoryCategory, MemoryRecord
from app.schemas.memory import ALLOWED_MEMORY_CATEGORIES


class MemoryRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def ensure_default_categories(self) -> None:
        existing = {
            row.name
            for row in self.db.query(MemoryCategory).filter(MemoryCategory.is_system == True).all()  # noqa: E712
        }
        added = False
        for name in ALLOWED_MEMORY_CATEGORIES:
            if name not in existing:
                self.db.add(MemoryCategory(name=name, is_system=True, description=f"{name} category"))
                added = True
        if added:
            self.db.commit()

    def list_by_user(self, user_id: str, category: Optional[str] = None) -> List[MemoryRecord]:
        q = self.db.query(MemoryRecord).filter(MemoryRecord.user_id == user_id)
        if category:
            q = q.filter(MemoryRecord.category == category)
        return q.order_by(MemoryRecord.updated_at.desc()).all()

    def get_by_id_for_user(self, memory_id: str, user_id: str) -> Optional[MemoryRecord]:
        return (
            self.db.query(MemoryRecord)
            .filter(MemoryRecord.id == memory_id, MemoryRecord.user_id == user_id)
            .first()
        )

    def create(
        self,
        user_id: str,
        category: str,
        content: str,
        expires_at=None,
        family_shared: bool = False,
    ) -> MemoryRecord:
        self.ensure_default_categories()
        rec = MemoryRecord(
            user_id=user_id,
            category=category,
            content=content,
            expires_at=expires_at,
            family_shared=family_shared,
        )
        self.db.add(rec)
        self.db.flush()
        self.db.add(
            MemoryAuditEvent(
                user_id=user_id,
                memory_id=rec.id,
                action="CREATED",
                category=category,
                detail="Explicit user memory saved",
            )
        )
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def update(self, rec: MemoryRecord) -> MemoryRecord:
        self.db.add(
            MemoryAuditEvent(
                user_id=rec.user_id,
                memory_id=rec.id,
                action="UPDATED",
                category=rec.category,
                detail="Memory updated by owner",
            )
        )
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def delete_one(self, rec: MemoryRecord) -> None:
        self.db.add(
            MemoryAuditEvent(
                user_id=rec.user_id,
                memory_id=rec.id,
                action="DELETED",
                category=rec.category,
                detail="Memory deleted by owner",
            )
        )
        self.db.delete(rec)
        self.db.commit()

    def delete_all_for_user(self, user_id: str) -> int:
        count = self.db.query(MemoryRecord).filter(MemoryRecord.user_id == user_id).delete()
        self.db.add(
            MemoryAuditEvent(
                user_id=user_id,
                memory_id=None,
                action="CLEARED_ALL",
                category="ALL",
                detail=f"Cleared {count} memories",
            )
        )
        self.db.commit()
        return count

    def list_audit_events(self, user_id: str, limit: int = 30) -> List[MemoryAuditEvent]:
        return (
            self.db.query(MemoryAuditEvent)
            .filter(MemoryAuditEvent.user_id == user_id)
            .order_by(MemoryAuditEvent.created_at.desc())
            .limit(limit)
            .all()
        )
