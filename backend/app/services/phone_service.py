from typing import List, Optional
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.repositories.phone_repository import PhoneRepository
from app.schemas.phone import (
    AppPreferenceResponse,
    AppPreferenceUpsert,
    ContactAliasCreate,
    ContactAliasResponse,
    ContactAliasUpdate,
    PhonePreferencesResponse,
    PhonePreferencesUpdate,
)


class PhoneService:
    def __init__(self, db: Session) -> None:
        self.repo = PhoneRepository(db)

    def get_preferences(self, user_id: str) -> PhonePreferencesResponse:
        p = self.repo.get_or_create_preferences(user_id)
        return PhonePreferencesResponse(
            confirm_before_call=p.confirm_before_call,
            allow_volume_control=p.allow_volume_control,
            allow_media_control=p.allow_media_control,
            allow_contact_lookup=p.allow_contact_lookup,
            updated_at=p.updated_at.isoformat(),
        )

    def update_preferences(
        self, user_id: str, req: PhonePreferencesUpdate
    ) -> PhonePreferencesResponse:
        p = self.repo.get_or_create_preferences(user_id)
        if req.confirm_before_call is not None:
            p.confirm_before_call = req.confirm_before_call
        if req.allow_volume_control is not None:
            p.allow_volume_control = req.allow_volume_control
        if req.allow_media_control is not None:
            p.allow_media_control = req.allow_media_control
        if req.allow_contact_lookup is not None:
            p.allow_contact_lookup = req.allow_contact_lookup
        p = self.repo.save_preferences(p)
        return PhonePreferencesResponse(
            confirm_before_call=p.confirm_before_call,
            allow_volume_control=p.allow_volume_control,
            allow_media_control=p.allow_media_control,
            allow_contact_lookup=p.allow_contact_lookup,
            updated_at=p.updated_at.isoformat(),
        )

    def list_aliases(
        self, user_id: str, query: Optional[str] = None
    ) -> List[ContactAliasResponse]:
        return [
            ContactAliasResponse(
                id=a.id,
                nickname=a.nickname,
                contact_name=a.contact_name,
                phone_hint=a.phone_hint,
                created_at=a.created_at.isoformat(),
                updated_at=a.updated_at.isoformat(),
            )
            for a in self.repo.list_contact_aliases(user_id, query=query)
        ]

    def get_alias(self, user_id: str, alias_id: str) -> ContactAliasResponse:
        a = self.repo.get_alias_by_id(alias_id, user_id)
        if not a:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Contact alias not found.")
        return ContactAliasResponse(
            id=a.id,
            nickname=a.nickname,
            contact_name=a.contact_name,
            phone_hint=a.phone_hint,
            created_at=a.created_at.isoformat(),
            updated_at=a.updated_at.isoformat(),
        )

    def create_alias(self, user_id: str, req: ContactAliasCreate) -> ContactAliasResponse:
        nickname = req.nickname.strip()
        contact_name = req.contact_name.strip()
        if not nickname or not contact_name:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Nickname and contact_name cannot be empty.",
            )
        a = self.repo.create_alias(user_id, nickname, contact_name, req.phone_hint.strip())
        return ContactAliasResponse(
            id=a.id,
            nickname=a.nickname,
            contact_name=a.contact_name,
            phone_hint=a.phone_hint,
            created_at=a.created_at.isoformat(),
            updated_at=a.updated_at.isoformat(),
        )

    def update_alias(
        self, user_id: str, alias_id: str, req: ContactAliasUpdate
    ) -> ContactAliasResponse:
        a = self.repo.get_alias_by_id(alias_id, user_id)
        if not a:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Contact alias not found.")
        if req.nickname is not None:
            cleaned_nick = req.nickname.strip()
            if not cleaned_nick:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Nickname cannot be empty.",
                )
            a.nickname = cleaned_nick
        if req.contact_name is not None:
            cleaned_name = req.contact_name.strip()
            if not cleaned_name:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Contact name cannot be empty.",
                )
            a.contact_name = cleaned_name
        if req.phone_hint is not None:
            a.phone_hint = req.phone_hint.strip()
        a = self.repo.update_alias(a)
        return ContactAliasResponse(
            id=a.id,
            nickname=a.nickname,
            contact_name=a.contact_name,
            phone_hint=a.phone_hint,
            created_at=a.created_at.isoformat(),
            updated_at=a.updated_at.isoformat(),
        )

    def delete_alias(self, user_id: str, alias_id: str) -> None:
        a = self.repo.get_alias_by_id(alias_id, user_id)
        if not a:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Contact alias not found.")
        self.repo.delete_alias(a)

    def list_app_preferences(self, user_id: str) -> List[AppPreferenceResponse]:
        return [
            AppPreferenceResponse(
                id=x.id,
                alias_name=x.alias_name,
                package_name=x.package_name,
                is_favourite=x.is_favourite,
                updated_at=x.updated_at.isoformat(),
            )
            for x in self.repo.list_app_preferences(user_id)
        ]

    def upsert_app_preference(
        self, user_id: str, req: AppPreferenceUpsert
    ) -> AppPreferenceResponse:
        alias_name = req.alias_name.strip()
        package_name = req.package_name.strip()
        if not alias_name or not package_name:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="alias_name and package_name cannot be empty.",
            )
        x = self.repo.upsert_app_preference(
            user_id=user_id,
            alias_name=alias_name,
            package_name=package_name,
            is_favourite=req.is_favourite,
        )
        return AppPreferenceResponse(
            id=x.id,
            alias_name=x.alias_name,
            package_name=x.package_name,
            is_favourite=x.is_favourite,
            updated_at=x.updated_at.isoformat(),
        )

    def delete_app_preference(self, user_id: str, pref_id: str) -> None:
        x = self.repo.get_app_preference_by_id(pref_id, user_id)
        if not x:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="App preference not found.")
        self.repo.delete_app_preference(x)
