from fastapi import APIRouter

from app.api.v2.endpoints import (
    brain,
    environment,
    mobility_system,
    perception,
    productivity,
)

api_v2_router = APIRouter()
api_v2_router.include_router(perception.router)
api_v2_router.include_router(brain.router)
api_v2_router.include_router(productivity.router)
api_v2_router.include_router(environment.router)
api_v2_router.include_router(mobility_system.router)
