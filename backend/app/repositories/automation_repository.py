from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.security_automation import (
    AutomationAction,
    AutomationCondition,
    AutomationExecutionLog,
    AutomationRule,
)


class AutomationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_rules(self, user_id: str) -> List[AutomationRule]:
        return (
            self.db.query(AutomationRule)
            .filter(AutomationRule.user_id == user_id)
            .order_by(AutomationRule.created_at.desc())
            .all()
        )

    def get_rule(self, rule_id: str, user_id: str) -> Optional[AutomationRule]:
        return (
            self.db.query(AutomationRule)
            .filter(AutomationRule.id == rule_id, AutomationRule.user_id == user_id)
            .first()
        )

    def get_conditions(self, rule_id: str) -> List[AutomationCondition]:
        return (
            self.db.query(AutomationCondition)
            .filter(AutomationCondition.rule_id == rule_id)
            .all()
        )

    def get_actions(self, rule_id: str) -> List[AutomationAction]:
        return (
            self.db.query(AutomationAction)
            .filter(AutomationAction.rule_id == rule_id)
            .all()
        )

    def create_rule(
        self,
        rule: AutomationRule,
        conditions: List[AutomationCondition],
        actions: List[AutomationAction],
    ) -> AutomationRule:
        self.db.add(rule)
        self.db.flush()
        for c in conditions:
            c.rule_id = rule.id
            self.db.add(c)
        for a in actions:
            a.rule_id = rule.id
            self.db.add(a)
        self.db.commit()
        self.db.refresh(rule)
        return rule

    def replace_conditions_and_actions(
        self,
        rule_id: str,
        conditions: Optional[List[AutomationCondition]],
        actions: Optional[List[AutomationAction]],
    ) -> None:
        if conditions is not None:
            self.db.query(AutomationCondition).filter(
                AutomationCondition.rule_id == rule_id
            ).delete()
            for c in conditions:
                c.rule_id = rule_id
                self.db.add(c)
        if actions is not None:
            self.db.query(AutomationAction).filter(
                AutomationAction.rule_id == rule_id
            ).delete()
            for a in actions:
                a.rule_id = rule_id
                self.db.add(a)

    def save(self, rule: AutomationRule) -> AutomationRule:
        self.db.commit()
        self.db.refresh(rule)
        return rule

    def delete(self, rule: AutomationRule) -> None:
        self.db.delete(rule)
        self.db.commit()

    def add_execution_log(
        self, rule_id: str, user_id: str, status: str, summary: str
    ) -> AutomationExecutionLog:
        log = AutomationExecutionLog(
            rule_id=rule_id,
            user_id=user_id,
            status=status,
            summary=summary[:255],
        )
        self.db.add(log)
        self.db.commit()
        self.db.refresh(log)
        return log

    def list_execution_history(self, user_id: str, limit: int = 50) -> List[AutomationExecutionLog]:
        return (
            self.db.query(AutomationExecutionLog)
            .filter(AutomationExecutionLog.user_id == user_id)
            .order_by(AutomationExecutionLog.executed_at.desc())
            .limit(limit)
            .all()
        )
