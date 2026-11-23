from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.phone_messaging import MessagingPreference, MessagingTemplate


class MessagingRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_or_create_preferences(self, user_id: str) -> MessagingPreference:
        pref = (
            self.db.query(MessagingPreference)
            .filter(MessagingPreference.user_id == user_id)
            .first()
        )
        if pref is None:
            pref = MessagingPreference(user_id=user_id)
            self.db.add(pref)
            self.db.commit()
            self.db.refresh(pref)
        return pref

    def save_preferences(self, pref: MessagingPreference) -> MessagingPreference:
        self.db.commit()
        self.db.refresh(pref)
        return pref

    def list_templates(self, user_id: str) -> List[MessagingTemplate]:
        return (
            self.db.query(MessagingTemplate)
            .filter(MessagingTemplate.user_id == user_id)
            .order_by(MessagingTemplate.updated_at.desc())
            .all()
        )

    def get_template_by_id(self, template_id: str, user_id: str) -> Optional[MessagingTemplate]:
        return (
            self.db.query(MessagingTemplate)
            .filter(MessagingTemplate.id == template_id, MessagingTemplate.user_id == user_id)
            .first()
        )

    def create_template(
        self, user_id: str, title: str, language: str, body: str
    ) -> MessagingTemplate:
        tmpl = MessagingTemplate(
            user_id=user_id,
            title=title.strip(),
            language=language.strip(),
            body=body.strip(),
        )
        self.db.add(tmpl)
        self.db.commit()
        self.db.refresh(tmpl)
        return tmpl

    def update_template(self, tmpl: MessagingTemplate) -> MessagingTemplate:
        self.db.commit()
        self.db.refresh(tmpl)
        return tmpl

    def delete_template(self, tmpl: MessagingTemplate) -> None:
        self.db.delete(tmpl)
        self.db.commit()
