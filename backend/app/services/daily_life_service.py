from typing import List, Optional
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.models.daily_life import (
    CalendarEvent,
    DailyRoutine,
    Reminder,
    TaskItem,
    TimetableEntry,
)
from app.repositories.daily_life_repository import DailyLifeRepository
from app.schemas.daily_life import (
    CalendarEventCreate,
    CalendarEventResponse,
    CalendarEventUpdate,
    DailyRoutineCreate,
    DailyRoutineResponse,
    NotificationPreferencesResponse,
    NotificationPreferencesUpdate,
    ReminderCreate,
    ReminderResponse,
    ReminderUpdate,
    TaskCreate,
    TaskResponse,
    TaskUpdate,
    TimetableCreate,
    TimetableResponse,
    TimetableUpdate,
)


class DailyLifeService:
    def __init__(self, db: Session) -> None:
        self.repo = DailyLifeRepository(db)

    # Reminders
    def _reminder_dto(self, r: Reminder) -> ReminderResponse:
        return ReminderResponse(
            id=r.id,
            title=r.title,
            notes=r.notes,
            remind_at=r.remind_at.isoformat(),
            timezone_name=r.timezone_name,
            recurrence=r.recurrence,
            is_completed=r.is_completed,
            notify_enabled=r.notify_enabled,
            created_at=r.created_at.isoformat(),
            updated_at=r.updated_at.isoformat(),
        )

    def list_reminders(self, user_id: str) -> List[ReminderResponse]:
        return [self._reminder_dto(r) for r in self.repo.list_reminders(user_id)]

    def create_reminder(self, user_id: str, req: ReminderCreate) -> ReminderResponse:
        r = Reminder(
            user_id=user_id,
            title=req.title.strip(),
            notes=req.notes.strip(),
            remind_at=req.remind_at,
            timezone_name=req.timezone_name.strip(),
            recurrence=req.recurrence.upper().strip(),
            notify_enabled=req.notify_enabled,
        )
        return self._reminder_dto(self.repo.create_reminder(r))

    def update_reminder(
        self, user_id: str, reminder_id: str, req: ReminderUpdate
    ) -> ReminderResponse:
        r = self.repo.get_reminder(reminder_id, user_id)
        if not r:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reminder not found.")
        if req.title is not None:
            r.title = req.title.strip()
        if req.notes is not None:
            r.notes = req.notes.strip()
        if req.remind_at is not None:
            r.remind_at = req.remind_at
        if req.timezone_name is not None:
            r.timezone_name = req.timezone_name.strip()
        if req.recurrence is not None:
            r.recurrence = req.recurrence.upper().strip()
        if req.is_completed is not None:
            r.is_completed = req.is_completed
        if req.notify_enabled is not None:
            r.notify_enabled = req.notify_enabled
        return self._reminder_dto(self.repo.save(r))

    def delete_reminder(self, user_id: str, reminder_id: str) -> None:
        r = self.repo.get_reminder(reminder_id, user_id)
        if not r:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reminder not found.")
        self.repo.delete(r)

    # Tasks
    def _task_dto(self, t: TaskItem) -> TaskResponse:
        return TaskResponse(
            id=t.id,
            title=t.title,
            priority=t.priority,
            due_date=t.due_date.isoformat() if t.due_date else None,
            is_completed=t.is_completed,
            created_at=t.created_at.isoformat(),
            updated_at=t.updated_at.isoformat(),
        )

    def list_tasks(self, user_id: str) -> List[TaskResponse]:
        return [self._task_dto(t) for t in self.repo.list_tasks(user_id)]

    def create_task(self, user_id: str, req: TaskCreate) -> TaskResponse:
        t = TaskItem(
            user_id=user_id,
            title=req.title.strip(),
            priority=req.priority.upper().strip(),
            due_date=req.due_date,
        )
        return self._task_dto(self.repo.create_task(t))

    def update_task(self, user_id: str, task_id: str, req: TaskUpdate) -> TaskResponse:
        t = self.repo.get_task(task_id, user_id)
        if not t:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")
        if req.title is not None:
            t.title = req.title.strip()
        if req.priority is not None:
            t.priority = req.priority.upper().strip()
        if req.due_date is not None:
            t.due_date = req.due_date
        if req.is_completed is not None:
            t.is_completed = req.is_completed
        return self._task_dto(self.repo.save(t))

    def delete_task(self, user_id: str, task_id: str) -> None:
        t = self.repo.get_task(task_id, user_id)
        if not t:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")
        self.repo.delete(t)

    # Timetable
    def _timetable_dto(self, e: TimetableEntry) -> TimetableResponse:
        return TimetableResponse(
            id=e.id,
            day_of_week=e.day_of_week,
            start_time=e.start_time,
            end_time=e.end_time,
            subject_or_activity=e.subject_or_activity,
            location=e.location,
            created_at=e.created_at.isoformat(),
            updated_at=e.updated_at.isoformat(),
        )

    def list_timetable(self, user_id: str, day_of_week: Optional[str] = None) -> List[TimetableResponse]:
        return [self._timetable_dto(e) for e in self.repo.list_timetable(user_id, day_of_week)]

    def create_timetable_entry(self, user_id: str, req: TimetableCreate) -> TimetableResponse:
        e = TimetableEntry(
            user_id=user_id,
            day_of_week=req.day_of_week.strip(),
            start_time=req.start_time.strip(),
            end_time=req.end_time.strip(),
            subject_or_activity=req.subject_or_activity.strip(),
            location=req.location.strip(),
        )
        return self._timetable_dto(self.repo.create_timetable_entry(e))

    def update_timetable_entry(
        self, user_id: str, entry_id: str, req: TimetableUpdate
    ) -> TimetableResponse:
        e = self.repo.get_timetable_entry(entry_id, user_id)
        if not e:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Timetable entry not found.")
        if req.day_of_week is not None:
            e.day_of_week = req.day_of_week.strip()
        if req.start_time is not None:
            e.start_time = req.start_time.strip()
        if req.end_time is not None:
            e.end_time = req.end_time.strip()
        if req.subject_or_activity is not None:
            e.subject_or_activity = req.subject_or_activity.strip()
        if req.location is not None:
            e.location = req.location.strip()
        return self._timetable_dto(self.repo.save(e))

    def delete_timetable_entry(self, user_id: str, entry_id: str) -> None:
        e = self.repo.get_timetable_entry(entry_id, user_id)
        if not e:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Timetable entry not found.")
        self.repo.delete(e)

    # Calendar Events
    def _calendar_dto(self, ev: CalendarEvent) -> CalendarEventResponse:
        return CalendarEventResponse(
            id=ev.id,
            title=ev.title,
            description=ev.description,
            start_at=ev.start_at.isoformat(),
            end_at=ev.end_at.isoformat(),
            timezone_name=ev.timezone_name,
            created_at=ev.created_at.isoformat(),
            updated_at=ev.updated_at.isoformat(),
        )

    def list_calendar_events(self, user_id: str) -> List[CalendarEventResponse]:
        return [self._calendar_dto(ev) for ev in self.repo.list_calendar_events(user_id)]

    def create_calendar_event(
        self, user_id: str, req: CalendarEventCreate
    ) -> CalendarEventResponse:
        if req.end_at < req.start_at:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Calendar event end_at cannot be before start_at.",
            )
        ev = CalendarEvent(
            user_id=user_id,
            title=req.title.strip(),
            description=req.description.strip(),
            start_at=req.start_at,
            end_at=req.end_at,
            timezone_name=req.timezone_name.strip(),
        )
        return self._calendar_dto(self.repo.create_calendar_event(ev))

    def update_calendar_event(
        self, user_id: str, event_id: str, req: CalendarEventUpdate
    ) -> CalendarEventResponse:
        ev = self.repo.get_calendar_event(event_id, user_id)
        if not ev:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Calendar event not found.")
        if req.title is not None:
            ev.title = req.title.strip()
        if req.description is not None:
            ev.description = req.description.strip()
        if req.start_at is not None:
            ev.start_at = req.start_at
        if req.end_at is not None:
            ev.end_at = req.end_at
        if req.timezone_name is not None:
            ev.timezone_name = req.timezone_name.strip()
        return self._calendar_dto(self.repo.save(ev))

    def delete_calendar_event(self, user_id: str, event_id: str) -> None:
        ev = self.repo.get_calendar_event(event_id, user_id)
        if not ev:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Calendar event not found.")
        self.repo.delete(ev)

    # Routines & Notification Preferences
    def list_routines(self, user_id: str) -> List[DailyRoutineResponse]:
        return [
            DailyRoutineResponse(
                id=r.id,
                name=r.name,
                trigger_time=r.trigger_time,
                summary_prompt=r.summary_prompt,
                is_enabled=r.is_enabled,
                created_at=r.created_at.isoformat(),
                updated_at=r.updated_at.isoformat(),
            )
            for r in self.repo.list_routines(user_id)
        ]

    def create_routine(self, user_id: str, req: DailyRoutineCreate) -> DailyRoutineResponse:
        r = DailyRoutine(
            user_id=user_id,
            name=req.name.strip(),
            trigger_time=req.trigger_time.strip(),
            summary_prompt=req.summary_prompt.strip(),
            is_enabled=req.is_enabled,
        )
        r = self.repo.create_routine(r)
        return DailyRoutineResponse(
            id=r.id,
            name=r.name,
            trigger_time=r.trigger_time,
            summary_prompt=r.summary_prompt,
            is_enabled=r.is_enabled,
            created_at=r.created_at.isoformat(),
            updated_at=r.updated_at.isoformat(),
        )

    def get_notification_prefs(self, user_id: str) -> NotificationPreferencesResponse:
        p = self.repo.get_or_create_notification_prefs(user_id)
        return NotificationPreferencesResponse(
            morning_greeting_enabled=p.morning_greeting_enabled,
            evening_summary_enabled=p.evening_summary_enabled,
            reminder_notifications_enabled=p.reminder_notifications_enabled,
            battery_alert_threshold=p.battery_alert_threshold,
            updated_at=p.updated_at.isoformat(),
        )

    def update_notification_prefs(
        self, user_id: str, req: NotificationPreferencesUpdate
    ) -> NotificationPreferencesResponse:
        p = self.repo.get_or_create_notification_prefs(user_id)
        if req.morning_greeting_enabled is not None:
            p.morning_greeting_enabled = req.morning_greeting_enabled
        if req.evening_summary_enabled is not None:
            p.evening_summary_enabled = req.evening_summary_enabled
        if req.reminder_notifications_enabled is not None:
            p.reminder_notifications_enabled = req.reminder_notifications_enabled
        if req.battery_alert_threshold is not None:
            p.battery_alert_threshold = req.battery_alert_threshold
        p = self.repo.save(p)
        return NotificationPreferencesResponse(
            morning_greeting_enabled=p.morning_greeting_enabled,
            evening_summary_enabled=p.evening_summary_enabled,
            reminder_notifications_enabled=p.reminder_notifications_enabled,
            battery_alert_threshold=p.battery_alert_threshold,
            updated_at=p.updated_at.isoformat(),
        )
