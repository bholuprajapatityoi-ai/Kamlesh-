from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.daily_life import (
    CalendarEvent,
    DailyRoutine,
    NotificationPreference,
    Reminder,
    TaskItem,
    TimetableEntry,
)


class DailyLifeRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    # Reminders
    def list_reminders(self, user_id: str) -> List[Reminder]:
        return (
            self.db.query(Reminder)
            .filter(Reminder.user_id == user_id)
            .order_by(Reminder.remind_at.asc())
            .all()
        )

    def get_reminder(self, reminder_id: str, user_id: str) -> Optional[Reminder]:
        return (
            self.db.query(Reminder)
            .filter(Reminder.id == reminder_id, Reminder.user_id == user_id)
            .first()
        )

    def create_reminder(self, reminder: Reminder) -> Reminder:
        self.db.add(reminder)
        self.db.commit()
        self.db.refresh(reminder)
        return reminder

    def save(self, obj):
        self.db.commit()
        self.db.refresh(obj)
        return obj

    def delete(self, obj) -> None:
        self.db.delete(obj)
        self.db.commit()

    # Tasks
    def list_tasks(self, user_id: str) -> List[TaskItem]:
        return (
            self.db.query(TaskItem)
            .filter(TaskItem.user_id == user_id)
            .order_by(TaskItem.is_completed.asc(), TaskItem.created_at.desc())
            .all()
        )

    def get_task(self, task_id: str, user_id: str) -> Optional[TaskItem]:
        return (
            self.db.query(TaskItem)
            .filter(TaskItem.id == task_id, TaskItem.user_id == user_id)
            .first()
        )

    def create_task(self, task: TaskItem) -> TaskItem:
        self.db.add(task)
        self.db.commit()
        self.db.refresh(task)
        return task

    # Timetable
    def list_timetable(self, user_id: str, day_of_week: Optional[str] = None) -> List[TimetableEntry]:
        q = self.db.query(TimetableEntry).filter(TimetableEntry.user_id == user_id)
        if day_of_week:
            q = q.filter(TimetableEntry.day_of_week == day_of_week)
        return q.order_by(TimetableEntry.day_of_week.asc(), TimetableEntry.start_time.asc()).all()

    def get_timetable_entry(self, entry_id: str, user_id: str) -> Optional[TimetableEntry]:
        return (
            self.db.query(TimetableEntry)
            .filter(TimetableEntry.id == entry_id, TimetableEntry.user_id == user_id)
            .first()
        )

    def create_timetable_entry(self, entry: TimetableEntry) -> TimetableEntry:
        self.db.add(entry)
        self.db.commit()
        self.db.refresh(entry)
        return entry

    # Calendar Events
    def list_calendar_events(self, user_id: str) -> List[CalendarEvent]:
        return (
            self.db.query(CalendarEvent)
            .filter(CalendarEvent.user_id == user_id)
            .order_by(CalendarEvent.start_at.asc())
            .all()
        )

    def get_calendar_event(self, event_id: str, user_id: str) -> Optional[CalendarEvent]:
        return (
            self.db.query(CalendarEvent)
            .filter(CalendarEvent.id == event_id, CalendarEvent.user_id == user_id)
            .first()
        )

    def create_calendar_event(self, event: CalendarEvent) -> CalendarEvent:
        self.db.add(event)
        self.db.commit()
        self.db.refresh(event)
        return event

    # Routines & Notification Preferences
    def list_routines(self, user_id: str) -> List[DailyRoutine]:
        return (
            self.db.query(DailyRoutine)
            .filter(DailyRoutine.user_id == user_id)
            .order_by(DailyRoutine.trigger_time.asc())
            .all()
        )

    def create_routine(self, routine: DailyRoutine) -> DailyRoutine:
        self.db.add(routine)
        self.db.commit()
        self.db.refresh(routine)
        return routine

    def get_or_create_notification_prefs(self, user_id: str) -> NotificationPreference:
        pref = (
            self.db.query(NotificationPreference)
            .filter(NotificationPreference.user_id == user_id)
            .first()
        )
        if pref is None:
            pref = NotificationPreference(user_id=user_id)
            self.db.add(pref)
            self.db.commit()
            self.db.refresh(pref)
        return pref
