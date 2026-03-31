import httpx


async def send_guide_event_to_backend(payload: dict, backend_url: str) -> dict:
    async with httpx.AsyncClient(timeout=5.0) as client:
        response = await client.post(backend_url, json=payload)
        response.raise_for_status()

        content_type = response.headers.get("content-type", "")
        if "application/json" in content_type:
            return response.json()

        return {"status_code": response.status_code, "text": response.text}