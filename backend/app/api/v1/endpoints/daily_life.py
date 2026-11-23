from typing import List, Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
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
from app.services.daily_life_service import DailyLifeService

router = APIRouter(tags=["Daily Life Assistant (Phase 10)"])


# Reminders
@router.post("/reminders", response_model=ReminderResponse, status_code=status.HTTP_201_CREATED)
def create_reminder(
    payload: ReminderCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).create_reminder(user.id, payload)


@router.get("/reminders", response_model=List[ReminderResponse])
def list_reminders(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).list_reminders(user.id)


@router.put("/reminders/{reminder_id}", response_model=ReminderResponse)
def update_reminder(
    reminder_id: str,
    payload: ReminderUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).update_reminder(user.id, reminder_id, payload)


@router.delete("/reminders/{reminder_id}", response_model=MessageResponse)
def delete_reminder(
    reminder_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DailyLifeService(db).delete_reminder(user.id, reminder_id)
    return MessageResponse(message="Reminder deleted.")


# Tasks
@router.post("/tasks", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
def create_task(
    payload: TaskCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).create_task(user.id, payload)


@router.get("/tasks", response_model=List[TaskResponse])
def list_tasks(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).list_tasks(user.id)


@router.put("/tasks/{task_id}", response_model=TaskResponse)
def update_task(
    task_id: str,
    payload: TaskUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).update_task(user.id, task_id, payload)


@router.delete("/tasks/{task_id}", response_model=MessageResponse)
def delete_task(
    task_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DailyLifeService(db).delete_task(user.id, task_id)
    return MessageResponse(message="Task deleted.")


# Timetable
@router.post("/timetable", response_model=TimetableResponse, status_code=status.HTTP_201_CREATED)
def create_timetable_entry(
    payload: TimetableCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).create_timetable_entry(user.id, payload)


@router.get("/timetable", response_model=List[TimetableResponse])
def list_timetable(
    day_of_week: Optional[str] = Query(default=None),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).list_timetable(user.id, day_of_week)


@router.put("/timetable/{entry_id}", response_model=TimetableResponse)
def update_timetable_entry(
    entry_id: str,
    payload: TimetableUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).update_timetable_entry(user.id, entry_id, payload)


@router.delete("/timetable/{entry_id}", response_model=MessageResponse)
def delete_timetable_entry(
    entry_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DailyLifeService(db).delete_timetable_entry(user.id, entry_id)
    return MessageResponse(message="Timetable entry deleted.")


# Calendar Events
@router.post(
    "/calendar/events", response_model=CalendarEventResponse, status_code=status.HTTP_201_CREATED
)
def create_calendar_event(
    payload: CalendarEventCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).create_calendar_event(user.id, payload)


@router.get("/calendar/events", response_model=List[CalendarEventResponse])
def list_calendar_events(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).list_calendar_events(user.id)


@router.put("/calendar/events/{event_id}", response_model=CalendarEventResponse)
def update_calendar_event(
    event_id: str,
    payload: CalendarEventUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).update_calendar_event(user.id, event_id, payload)


@router.delete("/calendar/events/{event_id}", response_model=MessageResponse)
def delete_calendar_event(
    event_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DailyLifeService(db).delete_calendar_event(user.id, event_id)
    return MessageResponse(message="Calendar event deleted.")


# Routines & Notification Preferences
@router.get("/daily-life/routines", response_model=List[DailyRoutineResponse])
def list_routines(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).list_routines(user.id)


@router.post(
    "/daily-life/routines", response_model=DailyRoutineResponse, status_code=status.HTTP_201_CREATED
)
def create_routine(
    payload: DailyRoutineCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).create_routine(user.id, payload)


@router.get("/daily-life/notifications", response_model=NotificationPreferencesResponse)
def get_notification_preferences(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).get_notification_prefs(user.id)


@router.put("/daily-life/notifications", response_model=NotificationPreferencesResponse)
def update_notification_preferences(
    payload: NotificationPreferencesUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DailyLifeService(db).update_notification_prefs(user.id, payload)
