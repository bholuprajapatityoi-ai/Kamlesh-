from typing import List, Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.memory import (
    MemoryAuditEventResponse,
    MemoryCreateRequest,
    MemoryListResponse,
    MemoryRecordResponse,
    MemoryUpdateRequest,
)
from app.services.memory_service import MemoryService

router = APIRouter(prefix="/memory", tags=["Personal Memory (Phase 7)"])


@router.post("", response_model=MemoryRecordResponse, status_code=status.HTTP_201_CREATED)
def create_memory(
    payload: MemoryCreateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MemoryService(db).create_memory(user.id, payload)


@router.get("", response_model=MemoryListResponse)
def list_memories(
    category: Optional[str] = Query(default=None),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MemoryService(db).list_memories(user.id, category)


@router.get("/audit", response_model=List[MemoryAuditEventResponse])
def list_memory_audit(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MemoryService(db).list_audit_events(user.id)


@router.get("/{memory_id}", response_model=MemoryRecordResponse)
def get_memory(
    memory_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MemoryService(db).get_memory(user.id, memory_id)


@router.put("/{memory_id}", response_model=MemoryRecordResponse)
def update_memory(
    memory_id: str,
    payload: MemoryUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MemoryService(db).update_memory(user.id, memory_id, payload)


@router.delete("/{memory_id}", response_model=MessageResponse)
def delete_memory(
    memory_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    MemoryService(db).delete_memory(user.id, memory_id)
    return MessageResponse(message="Memory record deleted.")


@router.delete("", response_model=MessageResponse)
def clear_all_memories(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    deleted = MemoryService(db).clear_all_memories(user.id)
    return MessageResponse(message=f"Cleared {deleted} personal memory record(s).")
