from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.automation import (
    AutomationCreate,
    AutomationExecutionLogRequest,
    AutomationExecutionLogResponse,
    AutomationResponse,
    AutomationUpdate,
)
from app.schemas.common import MessageResponse
from app.services.automation_service import AutomationService

router = APIRouter(prefix="/automations", tags=["Smart Automation (Phase 13)"])
singular_router = APIRouter(prefix="/automation", tags=["Smart Automation (Phase 13)"])


@router.post("", response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
def create_automation(
    payload: AutomationCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).create_rule(user.id, payload)


@router.get("", response_model=List[AutomationResponse])
def list_automations(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).list_rules(user.id)


@router.get("/history", response_model=List[AutomationExecutionLogResponse])
def get_automation_history(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).list_history(user.id)


@router.get("/{rule_id}", response_model=AutomationResponse)
def get_automation(
    rule_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).get_rule(user.id, rule_id)


@router.put("/{rule_id}", response_model=AutomationResponse)
def update_automation(
    rule_id: str,
    payload: AutomationUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).update_rule(user.id, rule_id, payload)


@router.delete("/{rule_id}", response_model=MessageResponse)
def delete_automation(
    rule_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    AutomationService(db).delete_rule(user.id, rule_id)
    return MessageResponse(message="Automation rule deleted.")


@router.post("/{rule_id}/enable", response_model=AutomationResponse)
def enable_automation(
    rule_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).set_enabled(user.id, rule_id, True)


@router.post("/{rule_id}/disable", response_model=AutomationResponse)
def disable_automation(
    rule_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).set_enabled(user.id, rule_id, False)


@router.post("/{rule_id}/logs", response_model=AutomationExecutionLogResponse)
def log_automation_execution(
    rule_id: str,
    payload: AutomationExecutionLogRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return AutomationService(db).record_execution(user.id, rule_id, payload)


singular_router.add_api_route("", create_automation, methods=["POST"], response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
singular_router.add_api_route("", list_automations, methods=["GET"], response_model=List[AutomationResponse])
singular_router.add_api_route("/history", get_automation_history, methods=["GET"], response_model=List[AutomationExecutionLogResponse])
singular_router.add_api_route("/{rule_id}", get_automation, methods=["GET"], response_model=AutomationResponse)
singular_router.add_api_route("/{rule_id}", update_automation, methods=["PUT"], response_model=AutomationResponse)
singular_router.add_api_route("/{rule_id}", delete_automation, methods=["DELETE"], response_model=MessageResponse)
singular_router.add_api_route("/{rule_id}/enable", enable_automation, methods=["POST"], response_model=AutomationResponse)
singular_router.add_api_route("/{rule_id}/disable", disable_automation, methods=["POST"], response_model=AutomationResponse)
singular_router.add_api_route("/{rule_id}/logs", log_automation_execution, methods=["POST"], response_model=AutomationExecutionLogResponse)
