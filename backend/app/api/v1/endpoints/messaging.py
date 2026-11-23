from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.messaging import (
    MessagingPreferencesResponse,
    MessagingPreferencesUpdate,
    MessagingTemplateCreate,
    MessagingTemplateResponse,
    MessagingTemplateUpdate,
)
from app.services.messaging_service import MessagingService

router = APIRouter(prefix="/messaging", tags=["WhatsApp Assistant (Phase 9)"])


@router.get("/preferences", response_model=MessagingPreferencesResponse)
def get_messaging_preferences(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MessagingService(db).get_preferences(user.id)


@router.put("/preferences", response_model=MessagingPreferencesResponse)
def update_messaging_preferences(
    payload: MessagingPreferencesUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MessagingService(db).update_preferences(user.id, payload)


@router.get("/templates", response_model=List[MessagingTemplateResponse])
def list_messaging_templates(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MessagingService(db).list_templates(user.id)


@router.post(
    "/templates", response_model=MessagingTemplateResponse, status_code=status.HTTP_201_CREATED
)
def create_messaging_template(
    payload: MessagingTemplateCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MessagingService(db).create_template(user.id, payload)


@router.put("/templates/{template_id}", response_model=MessagingTemplateResponse)
def update_messaging_template(
    template_id: str,
    payload: MessagingTemplateUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return MessagingService(db).update_template(user.id, template_id, payload)


@router.delete("/templates/{template_id}", response_model=MessageResponse)
def delete_messaging_template(
    template_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    MessagingService(db).delete_template(user.id, template_id)
    return MessageResponse(message="Messaging template deleted.")
