from typing import List
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.repositories.messaging_repository import MessagingRepository
from app.schemas.messaging import (
    MessagingPreferencesResponse,
    MessagingPreferencesUpdate,
    MessagingTemplateCreate,
    MessagingTemplateResponse,
    MessagingTemplateUpdate,
)


class MessagingService:
    def __init__(self, db: Session) -> None:
        self.repo = MessagingRepository(db)

    def get_preferences(self, user_id: str) -> MessagingPreferencesResponse:
        p = self.repo.get_or_create_preferences(user_id)
        return MessagingPreferencesResponse(
            default_language=p.default_language,
            always_confirm_before_open=p.always_confirm_before_open,
            signature_text=p.signature_text,
            updated_at=p.updated_at.isoformat(),
        )

    def update_preferences(
        self, user_id: str, req: MessagingPreferencesUpdate
    ) -> MessagingPreferencesResponse:
        p = self.repo.get_or_create_preferences(user_id)
        if req.default_language is not None:
            p.default_language = req.default_language.strip()
        if req.always_confirm_before_open is not None:
            p.always_confirm_before_open = req.always_confirm_before_open
        if req.signature_text is not None:
            p.signature_text = req.signature_text.strip()
        p = self.repo.save_preferences(p)
        return MessagingPreferencesResponse(
            default_language=p.default_language,
            always_confirm_before_open=p.always_confirm_before_open,
            signature_text=p.signature_text,
            updated_at=p.updated_at.isoformat(),
        )

    def list_templates(self, user_id: str) -> List[MessagingTemplateResponse]:
        return [
            MessagingTemplateResponse(
                id=t.id,
                title=t.title,
                language=t.language,
                body=t.body,
                created_at=t.created_at.isoformat(),
                updated_at=t.updated_at.isoformat(),
            )
            for t in self.repo.list_templates(user_id)
        ]

    def create_template(
        self, user_id: str, req: MessagingTemplateCreate
    ) -> MessagingTemplateResponse:
        if any(k in req.body.lower() for k in ("password", "otp", "auth code", "token")):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="WhatsApp credentials or OTP codes cannot be stored in templates.",
            )
        t = self.repo.create_template(user_id, req.title, req.language, req.body)
        return MessagingTemplateResponse(
            id=t.id,
            title=t.title,
            language=t.language,
            body=t.body,
            created_at=t.created_at.isoformat(),
            updated_at=t.updated_at.isoformat(),
        )

    def update_template(
        self, user_id: str, template_id: str, req: MessagingTemplateUpdate
    ) -> MessagingTemplateResponse:
        t = self.repo.get_template_by_id(template_id, user_id)
        if not t:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found.")
        if req.title is not None:
            t.title = req.title.strip()
        if req.language is not None:
            t.language = req.language.strip()
        if req.body is not None:
            t.body = req.body.strip()
        t = self.repo.update_template(t)
        return MessagingTemplateResponse(
            id=t.id,
            title=t.title,
            language=t.language,
            body=t.body,
            created_at=t.created_at.isoformat(),
            updated_at=t.updated_at.isoformat(),
        )

    def delete_template(self, user_id: str, template_id: str) -> None:
        t = self.repo.get_template_by_id(template_id, user_id)
        if not t:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Template not found.")
        self.repo.delete_template(t)
