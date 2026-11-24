import asyncio
import base64
import io
import json
from typing import Any, Dict, List, Optional, Tuple
import httpx
from fastapi import HTTPException, status

from app.core.config import get_settings
from app.security.sanitizer import contains_secret_pattern, sanitize_text

_RETRYABLE_STATUS_CODES = {429, 500, 502, 503, 504}


async def _openai_post_with_retry(
    url: str,
    headers: Dict[str, str],
    payload: Dict[str, Any],
    timeout_seconds: float,
    service_label: str,
    max_retries: int = 2,
) -> httpx.Response:
    last_exc: Optional[Exception] = None
    last_resp: Optional[httpx.Response] = None

    for attempt in range(max_retries + 1):
        try:
            async with httpx.AsyncClient(timeout=timeout_seconds) as client:
                resp = await client.post(url, headers=headers, json=payload)
            if resp.status_code not in _RETRYABLE_STATUS_CODES or attempt == max_retries:
                return resp
            last_resp = resp
        except httpx.TimeoutException as exc:
            last_exc = exc
            if attempt == max_retries:
                raise HTTPException(
                    status_code=504,
                    detail=f"{service_label} request timed out.",
                ) from exc
        except httpx.RequestError as exc:
            last_exc = exc
            if attempt == max_retries:
                raise HTTPException(
                    status_code=502,
                    detail=f"{service_label} connection error: {type(exc).__name__}",
                ) from exc

        await asyncio.sleep(0.5 * (2**attempt))

    if last_resp is not None:
        return last_resp
    raise HTTPException(
        status_code=502,
        detail=f"{service_label} failed: {type(last_exc).__name__ if last_exc else 'unknown'}",
    )


async def call_openai_text(system_prompt: str, user_prompt: str, max_tokens: int = 700) -> str:
    settings = get_settings()
    if not settings.openai_configured:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Online AI unavailable: OPENAI_API_KEY is not configured on the backend.",
        )

    headers = {
        "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.OPENAI_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": sanitize_text(user_prompt, 12000)},
        ],
        "temperature": 0.4,
        "max_tokens": max_tokens,
    }
    resp = await _openai_post_with_retry(
        url="https://api.openai.com/v1/chat/completions",
        headers=headers,
        payload=payload,
        timeout_seconds=settings.OPENAI_TIMEOUT_SECONDS,
        service_label="OpenAI upstream",
    )

    if resp.status_code == 429:
        raise HTTPException(status_code=429, detail="OpenAI rate limit exceeded.")
    if resp.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"OpenAI upstream returned HTTP {resp.status_code}.")

    choices = (resp.json() or {}).get("choices") or []
    if not choices:
        raise HTTPException(status_code=502, detail="OpenAI returned an empty response.")
    return (choices[0].get("message") or {}).get("content", "").strip()


async def call_openai_vision(image_base64: str, prompt: str, language: str) -> str:
    settings = get_settings()
    if not settings.openai_configured:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Vision AI requires OPENAI_API_KEY configured on the backend.",
        )

    clean_b64 = (image_base64 or "").strip()
    if clean_b64.startswith("data:"):
        clean_b64 = clean_b64.split(",", 1)[-1]
    if not clean_b64:
        raise HTTPException(status_code=400, detail="Valid image_base64 payload is required.")

    headers = {
        "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.OPENAI_VISION_MODEL,
        "messages": [
            {
                "role": "system",
                "content": (
                    f"You are GOLU Vision AI assisting Boss in {language}. "
                    "Describe objects, visible text, and answer accurately without inventing unseen details."
                ),
            },
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": sanitize_text(prompt, 1000)},
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{clean_b64}"}},
                ],
            },
        ],
        "max_tokens": 550,
    }
    resp = await _openai_post_with_retry(
        url="https://api.openai.com/v1/chat/completions",
        headers=headers,
        payload=payload,
        timeout_seconds=settings.OPENAI_TIMEOUT_SECONDS,
        service_label="OpenAI Vision",
    )

    if resp.status_code == 429:
        raise HTTPException(status_code=429, detail="OpenAI Vision rate limit exceeded.")
    if resp.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"OpenAI Vision returned HTTP {resp.status_code}.")

    choices = (resp.json() or {}).get("choices") or []
    if not choices:
        raise HTTPException(status_code=502, detail="OpenAI Vision returned empty content.")
    return (choices[0].get("message") or {}).get("content", "").strip()


async def call_openai_image(prompt: str) -> Tuple[str, str]:
    settings = get_settings()
    if not settings.openai_configured:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Image generation requires OPENAI_API_KEY configured on the backend.",
        )

    headers = {
        "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.OPENAI_IMAGE_MODEL,
        "prompt": sanitize_text(prompt, 1200),
        "n": 1,
        "size": "1024x1024",
    }
    resp = await _openai_post_with_retry(
        url="https://api.openai.com/v1/images/generations",
        headers=headers,
        payload=payload,
        timeout_seconds=max(settings.OPENAI_TIMEOUT_SECONDS, 45.0),
        service_label="Image generation",
    )

    if resp.status_code >= 400:
        detail = f"OpenAI Images API returned HTTP {resp.status_code}."
        try:
            err = resp.json().get("error", {}).get("message")
            if err:
                detail = str(err)[:240]
        except Exception:
            pass
        raise HTTPException(status_code=502, detail=detail)

    items = (resp.json() or {}).get("data") or []
    if not items:
        raise HTTPException(status_code=502, detail="OpenAI Images API returned no image URL.")
    first = items[0]
    return str(first.get("url") or ""), str(first.get("revised_prompt") or "")


def extract_text_from_file_bytes(file_base64: str, mime_type: str) -> Tuple[str, int]:
    raw_b64 = (file_base64 or "").strip()
    if "," in raw_b64 and raw_b64.startswith("data:"):
        raw_b64 = raw_b64.split(",", 1)[1]
    try:
        data = base64.b64decode(raw_b64, validate=True)
    except Exception as exc:
        raise HTTPException(status_code=400, detail="Invalid base64 document payload.") from exc

    size_bytes = len(data)
    if size_bytes > 4_000_000:
        raise HTTPException(status_code=413, detail="Document exceeds 4 MB safety limit.")

    if "pdf" in (mime_type or "").lower():
        try:
            from pypdf import PdfReader  # type: ignore

            reader = PdfReader(io.BytesIO(data))
            pages = [page.extract_text() or "" for page in reader.pages[:25]]
            text = "\n".join(p.strip() for p in pages if p.strip())
            return sanitize_text(text, 40000), size_bytes
        except ImportError:
            # Fallback if pypdf is not installed in runtime
            decoded = data.decode("utf-8", errors="ignore")
            return sanitize_text(decoded, 40000), size_bytes
        except Exception as exc:
            raise HTTPException(status_code=400, detail=f"Unable to parse PDF: {type(exc).__name__}") from exc

    decoded = data.decode("utf-8", errors="replace")
    return sanitize_text(decoded, 40000), size_bytes


def scrub_generated_code_secrets(code_text: str) -> Tuple[str, str]:
    if contains_secret_pattern(code_text):
        return (
            code_text,
            "Warning: Potential secret-like literal detected in snippet. Always inject secrets via environment variables or Secrets panel.",
        )
    return (
        code_text,
        "Verified: No hardcoded secret patterns detected. Never execute untrusted snippets automatically.",
    )


async def perform_legitimate_web_research(query: str) -> Tuple[str, List[Dict[str, str]], str]:
    settings = get_settings()
    clean_query = sanitize_text(query, 250)
    citations: List[Dict[str, str]] = []

    if settings.TAVILY_API_KEY.strip():
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.post(
                    "https://api.tavily.com/search",
                    json={
                        "api_key": settings.TAVILY_API_KEY.strip(),
                        "query": clean_query,
                        "search_depth": "basic",
                        "max_results": 5,
                    },
                )
            if resp.status_code == 200:
                results = (resp.json() or {}).get("results") or []
                for item in results:
                    if item.get("url"):
                        citations.append(
                            {
                                "title": str(item.get("title") or "Source")[:160],
                                "url": str(item.get("url")),
                                "snippet": sanitize_text(str(item.get("content") or ""), 400),
                            }
                        )
                if citations:
                    return "tavily-search", citations, ""
        except Exception:
            pass

    # Legitimate public Wikipedia Search API fallback (real citations, zero fabrication)
    try:
        async with httpx.AsyncClient(
            timeout=12.0,
            headers={"User-Agent": "GOLU-Assistant/2.0 (Android Research Client)"},
        ) as client:
            resp = await client.get(
                "https://en.wikipedia.org/w/api.php",
                params={
                    "action": "query",
                    "list": "search",
                    "srsearch": clean_query,
                    "format": "json",
                    "srlimit": 4,
                    "utf8": 1,
                },
            )
        if resp.status_code == 200:
            items = ((resp.json() or {}).get("query") or {}).get("search") or []
            for item in items:
                title = str(item.get("title") or "")
                if not title:
                    continue
                slug = title.replace(" ", "_")
                raw_snippet = (
                    str(item.get("snippet") or "")
                    .replace('<span class="searchmatch">', "")
                    .replace("</span>", "")
                )
                citations.append(
                    {
                        "title": title[:160],
                        "url": f"https://en.wikipedia.org/wiki/{slug}",
                        "snippet": sanitize_text(raw_snippet, 350),
                    }
                )
            return "wikipedia-api", citations, ""
        return "wikipedia-api", [], f"Search provider returned HTTP {resp.status_code}."
    except httpx.RequestError as exc:
        return "network-unavailable", [], f"Live web search unreachable ({type(exc).__name__})."


async def search_nearby_places_osm(destination: str, lat: Optional[float], lon: Optional[float]) -> Tuple[List[Dict[str, Any]], str]:
    headers = {"User-Agent": "GOLU-Assistant/2.0 (Travel Intelligence)"}
    places: List[Dict[str, Any]] = []
    try:
        async with httpx.AsyncClient(timeout=12.0, headers=headers) as client:
            query_text = destination.strip()
            params: Dict[str, Any] = {"q": query_text, "format": "json", "limit": 5}
            if lat is not None and lon is not None:
                params["viewbox"] = f"{lon - 0.15},{lat + 0.15},{lon + 0.15},{lat - 0.15}"
            resp = await client.get("https://nominatim.openstreetmap.org/search", params=params)
        if resp.status_code == 200:
            rows = resp.json() or []
            for row in rows[:5]:
                places.append(
                    {
                        "name": str(row.get("name") or row.get("display_name", "Place").split(",")[0])[:120],
                        "category": str(row.get("type") or row.get("class") or "place")[:50],
                        "address": str(row.get("display_name") or "")[:200],
                        "latitude": float(row["lat"]) if row.get("lat") else None,
                        "longitude": float(row["lon"]) if row.get("lon") else None,
                    }
                )
            return places, "OpenStreetMap Nominatim lookup complete. Live traffic, fare pricing & seat availability are not connected."
        return [], f"OpenStreetMap returned HTTP {resp.status_code}. Live route traffic unavailable."
    except httpx.RequestError as exc:
        return [], f"Location search unreachable ({type(exc).__name__}). Live route data unavailable."
