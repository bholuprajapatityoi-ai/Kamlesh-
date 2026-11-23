from typing import List, Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.phone import (
    AppPreferenceResponse,
    AppPreferenceUpsert,
    ContactAliasCreate,
    ContactAliasResponse,
    ContactAliasUpdate,
    PhonePreferencesResponse,
    PhonePreferencesUpdate,
)
from app.services.phone_service import PhoneService

router = APIRouter(prefix="/phone", tags=["Smart Phone Control (Phase 8)"])


@router.get("/preferences", response_model=PhonePreferencesResponse)
def get_phone_preferences(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).get_preferences(user.id)


@router.put("/preferences", response_model=PhonePreferencesResponse)
def update_phone_preferences(
    payload: PhonePreferencesUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).update_preferences(user.id, payload)


@router.get("/contact-aliases", response_model=List[ContactAliasResponse])
def list_contact_aliases(
    q: Optional[str] = Query(default=None, description="Optional case-insensitive filter on nickname or contact_name"),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).list_aliases(user.id, query=q)


@router.get("/contact-aliases/{alias_id}", response_model=ContactAliasResponse)
def get_contact_alias(
    alias_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).get_alias(user.id, alias_id)


@router.post(
    "/contact-aliases", response_model=ContactAliasResponse, status_code=status.HTTP_201_CREATED
)
def create_contact_alias(
    payload: ContactAliasCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).create_alias(user.id, payload)


@router.put("/contact-aliases/{alias_id}", response_model=ContactAliasResponse)
def update_contact_alias(
    alias_id: str,
    payload: ContactAliasUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).update_alias(user.id, alias_id, payload)


@router.delete("/contact-aliases/{alias_id}", response_model=MessageResponse)
def delete_contact_alias(
    alias_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    PhoneService(db).delete_alias(user.id, alias_id)
    return MessageResponse(message="Contact alias deleted.")


@router.get("/app-preferences", response_model=List[AppPreferenceResponse])
def list_app_preferences(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).list_app_preferences(user.id)


@router.put("/app-preferences", response_model=AppPreferenceResponse)
def upsert_app_preference(
    payload: AppPreferenceUpsert,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return PhoneService(db).upsert_app_preference(user.id, payload)


@router.delete("/app-preferences/{pref_id}", response_model=MessageResponse)
def delete_app_preference(
    pref_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    PhoneService(db).delete_app_preference(user.id, pref_id)
    return MessageResponse(message="App preference deleted.")
