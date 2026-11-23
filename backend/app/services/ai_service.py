import uuid
from sqlalchemy.orm import Session

from app.repositories.daily_life_repository import DailyLifeRepository
from app.repositories.memory_repository import MemoryRepository
from app.schemas.ai import AiChatRequest, AiChatResponse
from app.security.sanitizer import sanitize_text
from app.services.context_manager import context_manager
from app.services.conversation_service import conversation_service
from app.services.intent_classifier import intent_classifier
from app.services.offline_command_service import offline_command_service


class AiService:
    async def process_chat(self, db: Session, user_id: str, req: AiChatRequest) -> AiChatResponse:
        clean_message = sanitize_text(req.message, 2000)
        session_id = req.session_id or str(uuid.uuid4())
        context_key = f"{user_id}:{session_id}"

        intent, recommended_route, _ = intent_classifier.classify(clean_message)

        if intent == "MEMORY_SAVE_REQUEST":
            cleaned = (
                clean_message.replace("GOLU,", "")
                .replace("golu,", "")
                .replace("ye yaad rakhna", "")
                .replace("yaad rakhna", "")
                .replace("remember this", "")
                .replace("remember that", "")
                .strip(" :,-")
            )
            if len(cleaned) < 4:
                return AiChatResponse(
                    reply="Boss, kya yaad rakhna hai? Kripya poori jaankari batayein (aur confirm karein ki main ise Personal Memory mein save karoon).",
                    route="OFFLINE_ENGINE",
                    intent=intent,
                    session_id=session_id,
                    requires_confirmation=False,
                    provider="offline",
                )
            return AiChatResponse(
                reply=(
                    f'Boss, kya aap chahte hain ki main yeh Personal Memory mein save karoon: "{cleaned}"? '
                    "Confirm karne par hi save hoga."
                ),
                route="OFFLINE_ENGINE",
                intent=intent,
                session_id=session_id,
                requires_confirmation=True,
                provider="offline",
            )

        if intent == "DAILY_LIFE_QUERY":
            dl_repo = DailyLifeRepository(db)
            t = clean_message.lower()
            if "timetable" in t or "schedule" in t:
                entries = dl_repo.list_timetable(user_id)
                if not entries:
                    reply = "Boss, aapka timetable abhi khali hai. Daily Life tab se entries add kar sakte hain."
                else:
                    summary = "; ".join(
                        f"{e.day_of_week} {e.start_time}-{e.end_time}: {e.subject_or_activity}"
                        for e in entries[:5]
                    )
                    reply = f"Boss, aapka timetable summary: {summary}."
                return AiChatResponse(
                    reply=reply,
                    route="OFFLINE_ENGINE",
                    intent=intent,
                    session_id=session_id,
                    provider="offline",
                )
            if "task" in t:
                tasks = [x for x in dl_repo.list_tasks(user_id) if not x.is_completed]
                reply = (
                    f"Boss, aapke {len(tasks)} pending tasks hain: "
                    + ", ".join(x.title for x in tasks[:4])
                    if tasks
                    else "Boss, koi pending task nahi hai!"
                )
                return AiChatResponse(
                    reply=reply,
                    route="OFFLINE_ENGINE",
                    intent=intent,
                    session_id=session_id,
                    provider="offline",
                )
            reminders = [r for r in dl_repo.list_reminders(user_id) if not r.is_completed]
            reply = (
                f"Boss, aapka agla reminder: {reminders[0].title} ({reminders[0].remind_at.isoformat()})."
                if reminders
                else "Boss, abhi koi upcoming reminder scheduled nahi hai."
            )
            return AiChatResponse(
                reply=reply,
                route="OFFLINE_ENGINE",
                intent=intent,
                session_id=session_id,
                provider="offline",
            )

        if intent == "SENSITIVE_ACTION":
            return AiChatResponse(
                reply="Boss, yeh sensitive action lag raha hai. Device par confirm kijiye before execution.",
                route="OFFLINE_ENGINE",
                intent=intent,
                session_id=session_id,
                requires_confirmation=True,
                provider="offline",
            )

        if recommended_route == "ANDROID_ACTION":
            return AiChatResponse(
                reply=(
                    "Boss, yeh device action Android par locally execute hota hai. "
                    "App mein Quick Action ya local voice command se chalaiye."
                ),
                route="ANDROID_ACTION",
                intent=intent,
                session_id=session_id,
                provider="offline",
            )

        offline_reply = offline_command_service.try_handle(clean_message, intent, req.language)
        if offline_reply is not None:
            context_manager.append_turn(context_key, clean_message, offline_reply)
            conversation_service.persist_turn_if_permitted(
                db=db,
                user_id=user_id,
                session_id=session_id,
                user_message=clean_message,
                assistant_reply=offline_reply,
                intent=intent,
                route="OFFLINE_ENGINE",
            )
            return AiChatResponse(
                reply=offline_reply,
                route="OFFLINE_ENGINE",
                intent=intent,
                session_id=session_id,
                provider="offline",
            )

        # Provide only the authenticated user's own explicit memories as non-sensitive context
        mem_items = MemoryRepository(db).list_by_user(user_id)[:6]
        user_prompt = clean_message
        if mem_items:
            mem_ctx = "; ".join(f"[{m.category}] {m.content}" for m in mem_items)
            user_prompt = f"User saved memories (private to this user): {mem_ctx}\n\nQuestion: {clean_message}"

        online_result = await conversation_service.handle_online_turn_with_usage(
            context_key=context_key,
            message=user_prompt,
            language=req.language,
            response_style=req.response_style,
        )
        conversation_service.persist_turn_if_permitted(
            db=db,
            user_id=user_id,
            session_id=session_id,
            user_message=clean_message,
            assistant_reply=online_result.reply,
            intent=intent,
            route=recommended_route,
        )
        return AiChatResponse(
            reply=online_result.reply,
            route=recommended_route,
            intent=intent,
            session_id=session_id,
            provider=online_result.provider,
            model=online_result.model,
            prompt_tokens=online_result.prompt_tokens,
            completion_tokens=online_result.completion_tokens,
            total_tokens=online_result.total_tokens,
        )


ai_service = AiService()
