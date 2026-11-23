from fastapi import APIRouter

from app.api.v1.endpoints import (
    ai,
    assistant,
    auth,
    automation,
    config,
    daily_life,
    devices,
    family,
    health,
    memory,
    messaging,
    notifications,
    phone,
    security,
    status,
    users,
    voice,
)

api_v1_router = APIRouter()
api_v1_router.include_router(health.router)
api_v1_router.include_router(status.router)
api_v1_router.include_router(config.router)
api_v1_router.include_router(auth.router)
api_v1_router.include_router(users.router)
api_v1_router.include_router(users.profile_router)
api_v1_router.include_router(voice.router)
api_v1_router.include_router(assistant.router)
api_v1_router.include_router(ai.router)
api_v1_router.include_router(ai.conversations_router)
api_v1_router.include_router(memory.router)
api_v1_router.include_router(phone.router)
api_v1_router.include_router(messaging.router)
api_v1_router.include_router(daily_life.router)
api_v1_router.include_router(devices.router)
api_v1_router.include_router(notifications.router)
api_v1_router.include_router(family.router)
api_v1_router.include_router(security.router)
api_v1_router.include_router(automation.router)
api_v1_router.include_router(automation.singular_router)
