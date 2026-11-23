from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.phone_messaging import AppPreference, ContactAlias, PhoneActionPreference


class PhoneRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_or_create_preferences(self, user_id: str) -> PhoneActionPreference:
        pref = (
            self.db.query(PhoneActionPreference)
            .filter(PhoneActionPreference.user_id == user_id)
            .first()
        )
        if pref is None:
            pref = PhoneActionPreference(user_id=user_id)
            self.db.add(pref)
            self.db.commit()
            self.db.refresh(pref)
        return pref

    def save_preferences(self, pref: PhoneActionPreference) -> PhoneActionPreference:
        self.db.commit()
        self.db.refresh(pref)
        return pref

    def list_contact_aliases(
        self, user_id: str, query: Optional[str] = None
    ) -> List[ContactAlias]:
        q = self.db.query(ContactAlias).filter(ContactAlias.user_id == user_id)
        if query and query.strip():
            term = f"%{query.strip()}%"
            q = q.filter(
                ContactAlias.nickname.ilike(term) | ContactAlias.contact_name.ilike(term)
            )
        return q.order_by(ContactAlias.nickname.asc()).all()

    def get_alias_by_id(self, alias_id: str, user_id: str) -> Optional[ContactAlias]:
        return (
            self.db.query(ContactAlias)
            .filter(ContactAlias.id == alias_id, ContactAlias.user_id == user_id)
            .first()
        )

    def create_alias(
        self, user_id: str, nickname: str, contact_name: str, phone_hint: str
    ) -> ContactAlias:
        alias = ContactAlias(
            user_id=user_id,
            nickname=nickname.strip(),
            contact_name=contact_name.strip(),
            phone_hint=phone_hint.strip(),
        )
        self.db.add(alias)
        self.db.commit()
        self.db.refresh(alias)
        return alias

    def update_alias(self, alias: ContactAlias) -> ContactAlias:
        self.db.commit()
        self.db.refresh(alias)
        return alias

    def delete_alias(self, alias: ContactAlias) -> None:
        self.db.delete(alias)
        self.db.commit()

    def list_app_preferences(self, user_id: str) -> List[AppPreference]:
        return (
            self.db.query(AppPreference)
            .filter(AppPreference.user_id == user_id)
            .order_by(AppPreference.alias_name.asc())
            .all()
        )

    def upsert_app_preference(
        self, user_id: str, alias_name: str, package_name: str, is_favourite: bool
    ) -> AppPreference:
        existing = (
            self.db.query(AppPreference)
            .filter(
                AppPreference.user_id == user_id,
                AppPreference.package_name == package_name,
            )
            .first()
        )
        if existing:
            existing.alias_name = alias_name
            existing.is_favourite = is_favourite
            self.db.commit()
            self.db.refresh(existing)
            return existing
        pref = AppPreference(
            user_id=user_id,
            alias_name=alias_name,
            package_name=package_name,
            is_favourite=is_favourite,
        )
        self.db.add(pref)
        self.db.commit()
        self.db.refresh(pref)
        return pref

    def get_app_preference_by_id(self, pref_id: str, user_id: str) -> Optional[AppPreference]:
        return (
            self.db.query(AppPreference)
            .filter(AppPreference.id == pref_id, AppPreference.user_id == user_id)
            .first()
        )

    def delete_app_preference(self, pref: AppPreference) -> None:
        self.db.delete(pref)
        self.db.commit()
