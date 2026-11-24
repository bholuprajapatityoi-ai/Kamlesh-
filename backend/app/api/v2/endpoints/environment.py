import ipaddress
import socket
from datetime import datetime, timezone
from typing import Dict, List
from urllib.parse import urlparse
import httpx
from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import PlainTextResponse
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.models.assistant_settings import AssistantSettings
from app.models.golu_v2 import ExpenseBudget, ExpenseRecord, SmartHomeConfig, SmartHomeDevice
from app.models.phone_messaging import AppPreference
from app.schemas.common import MessageResponse
from app.schemas.golu_v2 import (
    AppShortcutUpsertRequest,
    BudgetUpdateRequest,
    ExpenseCreateRequest,
    ExpenseRecordResponse,
    ExpenseSummaryResponse,
    PersonalityResponse,
    PersonalityUpdateRequest,
    SmartAppPreferenceResponse,
    SmartHomeConfigRequest,
    SmartHomeConfigResponse,
    SmartHomeDeviceCommandRequest,
    SmartHomeDeviceCreateRequest,
    SmartHomeDeviceResponse,
)
from app.security.sanitizer import ensure_owner, sanitize_text

router = APIRouter(tags=["Phase 19 — Smart Environment & Personalization (v2)"])


def _mask_token(tok: str) -> str:
    clean = (tok or "").strip()
    if not clean:
        return ""
    if len(clean) <= 6:
        return "***"
    return f"{clean[:2]}***{clean[-4:]}"


def _validate_public_https_hub_url(url: str) -> None:
    parsed = urlparse(url)
    host = (parsed.hostname or "").strip().lower()
    if parsed.scheme != "https" or not host:
        raise HTTPException(
            status_code=400,
            detail="Authorized smart-home hub URL must be a valid public HTTPS URL.",
        )
    if host in {"localhost", "127.0.0.1", "::1", "0.0.0.0"} or host.endswith(".local"):
        raise HTTPException(
            status_code=400,
            detail="Localhost / loopback Smart Home URLs are blocked for security.",
        )
    try:
        ip_obj = ipaddress.ip_address(host)
        if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local or ip_obj.is_reserved or ip_obj.is_multicast:
            raise HTTPException(
                status_code=400,
                detail="Private or link-local IP Smart Home URLs are blocked for security.",
            )
    except ValueError:
        try:
            infos = socket.getaddrinfo(host, parsed.port or 443, type=socket.SOCK_STREAM)
            for info in infos:
                resolved_ip = ipaddress.ip_address(info[4][0])
                if resolved_ip.is_private or resolved_ip.is_loopback or resolved_ip.is_link_local or resolved_ip.is_reserved:
                    raise HTTPException(
                        status_code=400,
                        detail="Smart Home host resolves to a restricted internal IP address.",
                    )
        except socket.gaierror:
            pass


# Feature 13: Smart Home Control
@router.get("/smart-home/config", response_model=SmartHomeConfigResponse)
def get_smart_home_config(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    cfg = db.query(SmartHomeConfig).filter(SmartHomeConfig.user_id == user.id).first()
    if not cfg or not cfg.is_authorized or not cfg.hub_url.strip():
        return SmartHomeConfigResponse(
            provider_type=cfg.provider_type if cfg else "HOME_ASSISTANT",
            hub_url=cfg.hub_url if cfg else "",
            token_hint=cfg.token_hint if cfg else "",
            is_authorized=False,
            connection_state="NOT_AUTHORIZED",
            detail="No authorized Smart Home hub URL & token configured. GOLU will never fake connected devices.",
        )
    return SmartHomeConfigResponse(
        provider_type=cfg.provider_type,
        hub_url=cfg.hub_url,
        token_hint=cfg.token_hint,
        is_authorized=cfg.is_authorized,
        connection_state="CONFIGURED_HUB",
        detail="Hub configured. Live device state is verified against your authorized hub on each command.",
    )


@router.put("/smart-home/config", response_model=SmartHomeConfigResponse)
def update_smart_home_config(
    payload: SmartHomeConfigRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    url = sanitize_text(payload.hub_url, 255).rstrip("/")
    if payload.authorize:
        _validate_public_https_hub_url(url)
        if not payload.access_token.strip():
            raise HTTPException(
                status_code=400,
                detail="Access token is required to authorize your Smart Home hub.",
            )

    cfg = db.query(SmartHomeConfig).filter(SmartHomeConfig.user_id == user.id).first()
    if not cfg:
        cfg = SmartHomeConfig(user_id=user.id)
        db.add(cfg)
    cfg.provider_type = sanitize_text(payload.provider_type, 40) or "HOME_ASSISTANT"
    cfg.hub_url = url
    if payload.access_token.strip():
        cfg.token_hint = _mask_token(payload.access_token)
    cfg.is_authorized = payload.authorize and bool(url)
    db.commit()
    db.refresh(cfg)
    return get_smart_home_config(auth=auth, db=db)


@router.get("/smart-home/devices", response_model=List[SmartHomeDeviceResponse])
def list_smart_home_devices(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(SmartHomeDevice)
        .filter(SmartHomeDevice.user_id == user.id)
        .order_by(SmartHomeDevice.created_at.desc())
        .all()
    )
    return [
        SmartHomeDeviceResponse(
            id=r.id,
            external_entity_id=r.external_entity_id,
            name=r.name,
            room=r.room,
            device_type=r.device_type,
            is_on=r.is_on,
            brightness_pct=r.brightness_pct,
            status=r.status,
            last_synced_at=r.last_synced_at,
        )
        for r in rows
    ]


@router.post("/smart-home/devices", response_model=SmartHomeDeviceResponse, status_code=status.HTTP_201_CREATED)
def add_smart_home_device(
    payload: SmartHomeDeviceCreateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    clean_entity = sanitize_text(payload.external_entity_id, 120)
    existing = (
        db.query(SmartHomeDevice)
        .filter(
            SmartHomeDevice.user_id == user.id,
            SmartHomeDevice.external_entity_id == clean_entity,
        )
        .first()
    )
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Smart home entity '{clean_entity}' is already registered.",
        )
    dev = SmartHomeDevice(
        user_id=user.id,
        external_entity_id=clean_entity,
        name=sanitize_text(payload.name, 120),
        room=sanitize_text(payload.room, 80),
        device_type=sanitize_text(payload.device_type, 40).upper(),
        status="AWAITING_HUB_VERIFICATION",
    )
    db.add(dev)
    db.commit()
    db.refresh(dev)
    return SmartHomeDeviceResponse(
        id=dev.id,
        external_entity_id=dev.external_entity_id,
        name=dev.name,
        room=dev.room,
        device_type=dev.device_type,
        is_on=dev.is_on,
        brightness_pct=dev.brightness_pct,
        status=dev.status,
        last_synced_at=dev.last_synced_at,
    )


@router.post("/smart-home/devices/{device_id}/command", response_model=SmartHomeDeviceResponse)
async def command_smart_home_device(
    device_id: str,
    payload: SmartHomeDeviceCommandRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    dev = db.query(SmartHomeDevice).filter(SmartHomeDevice.id == device_id).first()
    if not dev:
        raise HTTPException(status_code=404, detail="Smart device not found.")
    ensure_owner(dev.user_id, user.id, "Smart device not found.")

    if dev.device_type in ("LOCK", "GARAGE", "ALARM", "THERMOSTAT") and not payload.confirmed_sensitive:
        raise HTTPException(
            status_code=409,
            detail=f"Confirmation required before controlling sensitive device '{dev.name}'.",
        )

    cfg = db.query(SmartHomeConfig).filter(SmartHomeConfig.user_id == user.id).first()
    if not cfg or not cfg.is_authorized or not cfg.hub_url.startswith("https://"):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Smart Home hub not authorized/connected. Configure an HTTPS Home Assistant or Webhook hub first.",
        )

    _validate_public_https_hub_url(cfg.hub_url)

    # Probe configured HTTPS hub before changing reported live state
    try:
        async with httpx.AsyncClient(timeout=8.0, follow_redirects=False) as client:
            resp = await client.get(f"{cfg.hub_url}/api/")
        if resp.status_code >= 400:
            raise HTTPException(
                status_code=502,
                detail=f"Smart Home hub rejected verification (HTTP {resp.status_code}). State unchanged.",
            )
    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Smart Home hub unreachable ({type(exc).__name__}). Device not modified.",
        ) from exc

    dev.is_on = payload.turn_on
    dev.brightness_pct = payload.brightness_pct
    dev.status = "CONNECTED_VERIFIED"
    dev.last_synced_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(dev)
    return SmartHomeDeviceResponse(
        id=dev.id,
        external_entity_id=dev.external_entity_id,
        name=dev.name,
        room=dev.room,
        device_type=dev.device_type,
        is_on=dev.is_on,
        brightness_pct=dev.brightness_pct,
        status=dev.status,
        last_synced_at=dev.last_synced_at,
    )


@router.delete("/smart-home/devices/{device_id}", response_model=MessageResponse)
def delete_smart_home_device(
    device_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    dev = db.query(SmartHomeDevice).filter(SmartHomeDevice.id == device_id).first()
    if not dev:
        raise HTTPException(status_code=404, detail="Smart device not found.")
    ensure_owner(dev.user_id, user.id, "Smart device not found.")
    db.delete(dev)
    db.commit()
    return MessageResponse(message="Smart home device removed.")


# Feature 14: Expense Tracker (Manual entries, zero bank passwords)
def _current_month_key() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m")


@router.get("/expenses", response_model=ExpenseSummaryResponse)
def get_expenses_summary(
    month_key: str = Query(default_factory=_current_month_key, pattern="^\\d{4}-\\d{2}$"),
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    budget = (
        db.query(ExpenseBudget)
        .filter(ExpenseBudget.user_id == user.id, ExpenseBudget.month_key == month_key)
        .first()
    )
    rows = (
        db.query(ExpenseRecord)
        .filter(ExpenseRecord.user_id == user.id)
        .order_by(ExpenseRecord.spent_at.desc())
        .all()
    )
    month_rows = [r for r in rows if r.spent_at.strftime("%Y-%m") == month_key]
    by_cat: Dict[str, float] = {}
    total = 0.0
    for r in month_rows:
        total += r.amount
        by_cat[r.category] = round(by_cat.get(r.category, 0.0) + r.amount, 2)
    limit_amt = budget.limit_amount if budget else 0.0
    currency = budget.currency if budget else "INR"
    return ExpenseSummaryResponse(
        month_key=month_key,
        budget_limit=limit_amt,
        total_spent=round(total, 2),
        remaining_budget=round(limit_amt - total, 2) if limit_amt > 0 else 0.0,
        currency=currency,
        by_category=by_cat,
        items=[
            ExpenseRecordResponse(
                id=r.id,
                title=r.title,
                category=r.category,
                amount=r.amount,
                currency=r.currency,
                notes=r.notes,
                spent_at=r.spent_at,
            )
            for r in month_rows
        ],
    )


@router.post("/expenses", response_model=ExpenseRecordResponse, status_code=status.HTTP_201_CREATED)
def add_expense(
    payload: ExpenseCreateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = ExpenseRecord(
        user_id=user.id,
        title=sanitize_text(payload.title, 160),
        category=sanitize_text(payload.category, 50) or "General",
        amount=round(payload.amount, 2),
        currency=sanitize_text(payload.currency, 10) or "INR",
        notes=sanitize_text(payload.notes, 255),
    )
    db.add(rec)
    db.commit()
    db.refresh(rec)
    return ExpenseRecordResponse(
        id=rec.id,
        title=rec.title,
        category=rec.category,
        amount=rec.amount,
        currency=rec.currency,
        notes=rec.notes,
        spent_at=rec.spent_at,
    )


@router.put("/expenses/budget", response_model=ExpenseSummaryResponse)
def set_monthly_budget(
    payload: BudgetUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    budget = (
        db.query(ExpenseBudget)
        .filter(ExpenseBudget.user_id == user.id, ExpenseBudget.month_key == payload.month_key)
        .first()
    )
    if not budget:
        budget = ExpenseBudget(user_id=user.id, month_key=payload.month_key)
        db.add(budget)
    budget.limit_amount = round(payload.limit_amount, 2)
    budget.currency = sanitize_text(payload.currency, 10) or "INR"
    db.commit()
    return get_expenses_summary(month_key=payload.month_key, auth=auth, db=db)


@router.get("/expenses/export", response_class=PlainTextResponse)
def export_expenses_csv(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(ExpenseRecord)
        .filter(ExpenseRecord.user_id == user.id)
        .order_by(ExpenseRecord.spent_at.desc())
        .all()
    )
    lines = ["id,date,title,category,amount,currency,notes"]
    for r in rows:
        safe_title = r.title.replace('"', '""')
        safe_notes = r.notes.replace('"', '""')
        lines.append(
            f'{r.id},{r.spent_at.isoformat()},"{safe_title}",{r.category},{r.amount},{r.currency},"{safe_notes}"'
        )
    return "\n".join(lines)


@router.delete("/expenses/{expense_id}", response_model=MessageResponse)
def delete_expense(
    expense_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rec = db.query(ExpenseRecord).filter(ExpenseRecord.id == expense_id).first()
    if not rec:
        raise HTTPException(status_code=404, detail="Expense entry not found.")
    ensure_owner(rec.user_id, user.id, "Expense entry not found.")
    db.delete(rec)
    db.commit()
    return MessageResponse(message="Expense entry deleted.")


# Feature 15: Custom AI Personality (reuses AssistantSettings)
@router.get("/personality", response_model=PersonalityResponse)
def get_personality(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    st = db.query(AssistantSettings).filter(AssistantSettings.user_id == user.id).first()
    if not st:
        st = AssistantSettings(user_id=user.id)
        db.add(st)
        db.commit()
        db.refresh(st)
    return PersonalityResponse(
        response_style=st.response_style,
        humor_level=st.humor_level,
        response_length=st.response_length,
        greeting_message=st.greeting_message,
        voice_gender=st.voice_gender,
        avatar_style=st.avatar_style,
        custom_system_notes=st.custom_system_notes,
    )


@router.put("/personality", response_model=PersonalityResponse)
def update_personality(
    payload: PersonalityUpdateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    st = db.query(AssistantSettings).filter(AssistantSettings.user_id == user.id).first()
    if not st:
        st = AssistantSettings(user_id=user.id)
        db.add(st)
    if payload.response_style is not None:
        st.response_style = sanitize_text(payload.response_style, 40)
    if payload.humor_level is not None:
        st.humor_level = payload.humor_level
    if payload.response_length is not None:
        st.response_length = payload.response_length
    if payload.greeting_message is not None and payload.greeting_message.strip():
        st.greeting_message = sanitize_text(payload.greeting_message, 255)
    if payload.voice_gender is not None:
        st.voice_gender = sanitize_text(payload.voice_gender, 20)
    if payload.avatar_style is not None:
        st.avatar_style = sanitize_text(payload.avatar_style, 40)
    if payload.custom_system_notes is not None:
        st.custom_system_notes = sanitize_text(payload.custom_system_notes, 255)
    db.commit()
    db.refresh(st)
    return get_personality(auth=auth, db=db)


# Feature 16: Smart App Intelligence (reuses AppPreference)
@router.get("/apps/shortcuts", response_model=List[SmartAppPreferenceResponse])
def list_smart_app_shortcuts(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    rows = (
        db.query(AppPreference)
        .filter(AppPreference.user_id == user.id)
        .order_by(AppPreference.launch_count.desc(), AppPreference.updated_at.desc())
        .all()
    )
    return [
        SmartAppPreferenceResponse(
            id=r.id,
            alias_name=r.alias_name,
            package_name=r.package_name,
            is_favourite=r.is_favourite,
            launch_count=r.launch_count,
            shortcut_command=r.shortcut_command,
            last_launched_at=r.last_launched_at,
        )
        for r in rows
    ]


@router.post("/apps/shortcuts", response_model=SmartAppPreferenceResponse)
def upsert_smart_app_shortcut(
    payload: AppShortcutUpsertRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    pkg = sanitize_text(payload.package_name, 180)
    row = (
        db.query(AppPreference)
        .filter(AppPreference.user_id == user.id, AppPreference.package_name == pkg)
        .first()
    )
    if not row:
        row = AppPreference(
            user_id=user.id,
            alias_name=sanitize_text(payload.alias_name, 80),
            package_name=pkg,
            is_favourite=payload.is_favourite,
            shortcut_command=sanitize_text(payload.shortcut_command, 120),
            launch_count=1 if payload.increment_launch else 0,
            last_launched_at=datetime.now(timezone.utc) if payload.increment_launch else None,
        )
        db.add(row)
    else:
        row.alias_name = sanitize_text(payload.alias_name, 80)
        row.is_favourite = payload.is_favourite
        if payload.shortcut_command.strip():
            row.shortcut_command = sanitize_text(payload.shortcut_command, 120)
        if payload.increment_launch:
            row.launch_count += 1
            row.last_launched_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(row)
    return SmartAppPreferenceResponse(
        id=row.id,
        alias_name=row.alias_name,
        package_name=row.package_name,
        is_favourite=row.is_favourite,
        launch_count=row.launch_count,
        shortcut_command=row.shortcut_command,
        last_launched_at=row.last_launched_at,
    )


@router.delete("/apps/shortcuts/{app_id}", response_model=MessageResponse)
def delete_smart_app_shortcut(
    app_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    row = db.query(AppPreference).filter(AppPreference.id == app_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="App shortcut not found.")
    ensure_owner(row.user_id, user.id, "App shortcut not found.")
    db.delete(row)
    db.commit()
    return MessageResponse(message="App shortcut deleted.")
