from fastapi import APIRouter, Depends, Request, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.middleware.rate_limit import rate_limiter
from app.schemas.auth import (
    AuthResponse,
    ForgotPasswordRequest,
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
    ResetPasswordRequest,
    UserResponse,
    VerifyEmailConfirmRequest,
)
from app.schemas.common import MessageResponse
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(payload: RegisterRequest, request: Request, db: Session = Depends(get_db)):
    client_ip = request.client.host if request.client else "unknown"
    rate_limiter.check(f"register:{client_ip}", max_requests=get_settings().LOGIN_RATE_LIMIT_PER_MINUTE)
    return AuthService(db).register(
        full_name=payload.full_name,
        email=str(payload.email),
        password=payload.password,
        confirm_password=payload.confirm_password,
        ip_address=client_ip,
    )


@router.post("/login", response_model=AuthResponse)
def login(payload: LoginRequest, request: Request, db: Session = Depends(get_db)):
    client_ip = request.client.host if request.client else "unknown"
    rate_limiter.check(f"login:{client_ip}", max_requests=get_settings().LOGIN_RATE_LIMIT_PER_MINUTE)
    return AuthService(db).login(
        email=str(payload.email), password=payload.password, ip_address=client_ip
    )


@router.post("/refresh", response_model=AuthResponse)
def refresh_token(payload: RefreshRequest, db: Session = Depends(get_db)):
    return AuthService(db).refresh(payload.refresh_token)


@router.post("/logout", response_model=MessageResponse)
def logout(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, session = auth
    return AuthService(db).logout(session.id, user_id=user.id)


@router.post("/forgot-password", response_model=MessageResponse)
def forgot_password(
    payload: ForgotPasswordRequest, request: Request, db: Session = Depends(get_db)
):
    client_ip = request.client.host if request.client else "unknown"
    rate_limiter.check(f"forgot:{client_ip}", max_requests=5)
    return AuthService(db).forgot_password(str(payload.email))


@router.post("/reset-password", response_model=MessageResponse)
def reset_password(payload: ResetPasswordRequest, db: Session = Depends(get_db)):
    return AuthService(db).reset_password(
        token=payload.token,
        new_password=payload.new_password,
        confirm_password=payload.confirm_password,
    )


@router.post("/verify-email/request", response_model=MessageResponse)
def request_email_verification(
    request: Request,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    client_ip = request.client.host if request.client else "unknown"
    rate_limiter.check(f"verify_email:{user.id}:{client_ip}", max_requests=5)
    return AuthService(db).request_email_verification(user)


@router.post("/verify-email", response_model=MessageResponse)
def verify_email(payload: VerifyEmailConfirmRequest, db: Session = Depends(get_db)):
    return AuthService(db).confirm_email_verification(payload.token)


@router.get("/me", response_model=UserResponse)
def get_me(auth=Depends(get_current_user_and_session), db: Session = Depends(get_db)):
    user, _ = auth
    return AuthService(db).to_user_response(user)
