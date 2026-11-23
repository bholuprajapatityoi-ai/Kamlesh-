from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.auth import UserResponse
from app.schemas.user import UserProfileUpdateRequest
from app.services.user_service import UserService

router = APIRouter(prefix="/users", tags=["Users"])
profile_router = APIRouter(prefix="/profile", tags=["Profile"])


@router.get("/me", response_model=UserResponse)
def get_user_profile(auth=Depends(get_current_user_and_session), db: Session = Depends(get_db)):
    user, _ = auth
    return UserService(db).get_profile(user)


@router.put("/me", response_model=UserResponse)
def update_user_profile(
    payload: UserProfileUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return UserService(db).update_profile(user, payload)


@profile_router.get("", response_model=UserResponse)
def get_profile(auth=Depends(get_current_user_and_session), db: Session = Depends(get_db)):
    user, _ = auth
    return UserService(db).get_profile(user)


@profile_router.put("", response_model=UserResponse)
def update_profile(
    payload: UserProfileUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return UserService(db).update_profile(user, payload)
