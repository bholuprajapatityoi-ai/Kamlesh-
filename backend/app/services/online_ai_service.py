from dataclasses import dataclass
from typing import Dict, List
import httpx
from fastapi import HTTPException, status

from app.core.config import get_settings
from app.services.golu_v2_ai import _openai_post_with_retry
from app.services.response_formatter import response_formatter


@dataclass
class OnlineAiResult:
    reply: str
    provider: str
    model: str
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class OnlineAiService:
    async def generate_reply_with_usage(
        self,
        message: str,
        history: List[Dict[str, str]],
        language: str,
        response_style: str,
    ) -> OnlineAiResult:
        settings = get_settings()
        if not settings.openai_configured:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Online AI is not configured: OPENAI_API_KEY is missing on the server.",
            )

        system_prompt = response_formatter.build_system_prompt(language, response_style)
        messages = [
            {"role": "system", "content": system_prompt},
            *history,
            {"role": "user", "content": message},
        ]

        headers = {
            "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": settings.OPENAI_MODEL,
            "messages": messages,
            "temperature": 0.7,
            "max_tokens": 500,
        }

        resp = await _openai_post_with_retry(
            url="https://api.openai.com/v1/chat/completions",
            headers=headers,
            payload=payload,
            timeout_seconds=settings.OPENAI_TIMEOUT_SECONDS,
            service_label="OpenAI API",
        )

        if resp.status_code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="OpenAI upstream rate limit exceeded. Please retry shortly.",
            )
        if resp.status_code >= 400:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=f"OpenAI upstream returned HTTP {resp.status_code}.",
            )

        data = resp.json()
        choices = data.get("choices") or []
        if not choices:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="OpenAI returned an empty completion.",
            )
        content = choices[0].get("message", {}).get("content", "")
        usage = data.get("usage") or {}
        return OnlineAiResult(
            reply=response_formatter.format_reply(content),
            provider=settings.AI_PROVIDER,
            model=settings.OPENAI_MODEL,
            prompt_tokens=int(usage.get("prompt_tokens") or 0),
            completion_tokens=int(usage.get("completion_tokens") or 0),
            total_tokens=int(usage.get("total_tokens") or 0),
        )

    async def generate_reply(
        self,
        message: str,
        history: List[Dict[str, str]],
        language: str,
        response_style: str,
    ) -> str:
        res = await self.generate_reply_with_usage(
            message=message,
            history=history,
            language=language,
            response_style=response_style,
        )
        return res.reply


online_ai_service = OnlineAiService()
