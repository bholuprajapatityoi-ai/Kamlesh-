from typing import List
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.models.security_automation import (
    AutomationAction,
    AutomationCondition,
    AutomationRule,
)
from app.repositories.automation_repository import AutomationRepository
from app.schemas.automation import (
    ALLOWED_ACTION_TYPES,
    ALLOWED_CONDITION_TYPES,
    ALLOWED_TRIGGER_TYPES,
    AutomationActionDto,
    AutomationConditionDto,
    AutomationCreate,
    AutomationExecutionLogRequest,
    AutomationExecutionLogResponse,
    AutomationResponse,
    AutomationUpdate,
)


class AutomationService:
    def __init__(self, db: Session) -> None:
        self.repo = AutomationRepository(db)

    def _validate_rule_parts(
        self,
        trigger_type: str,
        conditions: List[AutomationConditionDto],
        actions: List[AutomationActionDto],
        requires_confirmation: bool,
        confirmed_sensitive: bool,
    ) -> None:
        if trigger_type.upper() not in ALLOWED_TRIGGER_TYPES:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Unsupported trigger_type '{trigger_type}'.",
            )
        for c in conditions:
            if c.condition_type.upper() not in ALLOWED_CONDITION_TYPES:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Unsupported condition_type '{c.condition_type}'.",
                )
        sensitive_action = False
        for a in actions:
            atype = a.action_type.upper()
            if atype not in ALLOWED_ACTION_TYPES:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Unsupported or unrestricted action_type '{a.action_type}' is not allowed.",
                )
            if atype in {"OPEN_APP", "ANDROID_ACTION"}:
                sensitive_action = True
        if (requires_confirmation or sensitive_action) and not confirmed_sensitive:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Explicit user confirmation is required before creating automations with sensitive device actions.",
            )

    def _to_dto(self, rule: AutomationRule) -> AutomationResponse:
        conds = [
            AutomationConditionDto(
                condition_type=c.condition_type, condition_value=c.condition_value
            )
            for c in self.repo.get_conditions(rule.id)
        ]
        acts = [
            AutomationActionDto(action_type=a.action_type, action_payload=a.action_payload)
            for a in self.repo.get_actions(rule.id)
        ]
        return AutomationResponse(
            id=rule.id,
            name=rule.name,
            trigger_type=rule.trigger_type,
            trigger_value=rule.trigger_value,
            is_enabled=rule.is_enabled,
            requires_confirmation=rule.requires_confirmation,
            conditions=conds,
            actions=acts,
            created_at=rule.created_at.isoformat(),
            updated_at=rule.updated_at.isoformat(),
        )

    def list_rules(self, user_id: str) -> List[AutomationResponse]:
        return [self._to_dto(r) for r in self.repo.list_rules(user_id)]

    def get_rule(self, user_id: str, rule_id: str) -> AutomationResponse:
        rule = self.repo.get_rule(rule_id, user_id)
        if not rule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Automation rule not found.")
        return self._to_dto(rule)

    def create_rule(self, user_id: str, req: AutomationCreate) -> AutomationResponse:
        self._validate_rule_parts(
            req.trigger_type,
            req.conditions,
            req.actions,
            req.requires_confirmation,
            req.confirmed_sensitive,
        )
        rule = AutomationRule(
            user_id=user_id,
            name=req.name.strip(),
            trigger_type=req.trigger_type.upper().strip(),
            trigger_value=req.trigger_value.strip(),
            is_enabled=req.is_enabled,
            requires_confirmation=req.requires_confirmation
            or any(a.action_type.upper() in {"OPEN_APP", "ANDROID_ACTION"} for a in req.actions),
        )
        conds = [
            AutomationCondition(
                condition_type=c.condition_type.upper().strip(),
                condition_value=c.condition_value.strip(),
            )
            for c in req.conditions
        ]
        acts = [
            AutomationAction(
                action_type=a.action_type.upper().strip(),
                action_payload=a.action_payload.strip(),
            )
            for a in req.actions
        ]
        return self._to_dto(self.repo.create_rule(rule, conds, acts))

    def update_rule(
        self, user_id: str, rule_id: str, req: AutomationUpdate
    ) -> AutomationResponse:
        rule = self.repo.get_rule(rule_id, user_id)
        if not rule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Automation rule not found.")
        if req.name is not None:
            rule.name = req.name.strip()
        if req.trigger_type is not None:
            if req.trigger_type.upper() not in ALLOWED_TRIGGER_TYPES:
                raise HTTPException(status_code=400, detail="Unsupported trigger_type.")
            rule.trigger_type = req.trigger_type.upper().strip()
        if req.trigger_value is not None:
            rule.trigger_value = req.trigger_value.strip()
        if req.is_enabled is not None:
            rule.is_enabled = req.is_enabled
        if req.requires_confirmation is not None:
            rule.requires_confirmation = req.requires_confirmation

        conds = (
            [
                AutomationCondition(
                    condition_type=c.condition_type.upper().strip(),
                    condition_value=c.condition_value.strip(),
                )
                for c in req.conditions
            ]
            if req.conditions is not None
            else None
        )
        if req.actions is not None:
            for a in req.actions:
                if a.action_type.upper() not in ALLOWED_ACTION_TYPES:
                    raise HTTPException(status_code=400, detail="Unsupported action_type.")
        acts = (
            [
                AutomationAction(
                    action_type=a.action_type.upper().strip(),
                    action_payload=a.action_payload.strip(),
                )
                for a in req.actions
            ]
            if req.actions is not None
            else None
        )
        self.repo.replace_conditions_and_actions(rule.id, conds, acts)
        return self._to_dto(self.repo.save(rule))

    def set_enabled(self, user_id: str, rule_id: str, enabled: bool) -> AutomationResponse:
        rule = self.repo.get_rule(rule_id, user_id)
        if not rule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Automation rule not found.")
        rule.is_enabled = enabled
        return self._to_dto(self.repo.save(rule))

    def delete_rule(self, user_id: str, rule_id: str) -> None:
        rule = self.repo.get_rule(rule_id, user_id)
        if not rule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Automation rule not found.")
        self.repo.delete(rule)

    def record_execution(
        self, user_id: str, rule_id: str, req: AutomationExecutionLogRequest
    ) -> AutomationExecutionLogResponse:
        rule = self.repo.get_rule(rule_id, user_id)
        if not rule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Automation rule not found.")
        log = self.repo.add_execution_log(rule.id, user_id, req.status.upper(), req.summary)
        return AutomationExecutionLogResponse(
            id=log.id,
            rule_id=log.rule_id,
            status=log.status,
            summary=log.summary,
            executed_at=log.executed_at.isoformat(),
        )

    def list_history(self, user_id: str) -> List[AutomationExecutionLogResponse]:
        return [
            AutomationExecutionLogResponse(
                id=l.id,
                rule_id=l.rule_id,
                status=l.status,
                summary=l.summary,
                executed_at=l.executed_at.isoformat(),
            )
            for l in self.repo.list_execution_history(user_id)
        ]
