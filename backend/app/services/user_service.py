from sqlalchemy.orm import Session

from app.models.user import User
from app.repositories.user_repository import UserRepository
from app.schemas.auth import UserResponse
from app.schemas.user import UserProfileUpdateRequest


class UserService:
    def __init__(self, db: Session) -> None:
        self.repo = UserRepository(db)

    def get_profile(self, user: User) -> UserResponse:
        profile = user.profile
        return UserResponse(
            id=user.id,
            full_name=profile.full_name if profile else "Boss",
            email=user.email,
            preferred_title=profile.preferred_title if profile else "Boss",
            preferred_language=profile.preferred_language if profile else "Hinglish",
            is_verified=user.is_verified,
            created_at=user.created_at.isoformat(),
        )

    def update_profile(self, user: User, payload: UserProfileUpdateRequest) -> UserResponse:
        updated = self.repo.update_profile(
            user=user,
            full_name=payload.full_name,
            preferred_title=payload.preferred_title,
            preferred_language=payload.preferred_language,
        )
        return self.get_profile(updated)
