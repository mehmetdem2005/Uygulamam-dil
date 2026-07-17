from __future__ import annotations

import os
import re
from collections.abc import AsyncIterator

import edge_tts
from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, field_validator

app = FastAPI(title="Uygulamam Dil Edge TTS", version="0.1.0")

_RATE = re.compile(r"^[+-]\d{1,3}%$")
_VOLUME = re.compile(r"^[+-]\d{1,3}%$")
_PITCH = re.compile(r"^[+-]\d{1,4}Hz$")


class SpeechRequest(BaseModel):
    text: str = Field(min_length=1, max_length=5_000)
    voice: str = Field(default="en-US-AriaNeural", min_length=3, max_length=100)
    rate: str = "+0%"
    volume: str = "+0%"
    pitch: str = "+0Hz"

    @field_validator("text")
    @classmethod
    def text_must_have_content(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            raise ValueError("text cannot be blank")
        return stripped

    @field_validator("rate")
    @classmethod
    def valid_rate(cls, value: str) -> str:
        if not _RATE.fullmatch(value):
            raise ValueError("rate must look like +0% or -25%")
        return value

    @field_validator("volume")
    @classmethod
    def valid_volume(cls, value: str) -> str:
        if not _VOLUME.fullmatch(value):
            raise ValueError("volume must look like +0% or -25%")
        return value

    @field_validator("pitch")
    @classmethod
    def valid_pitch(cls, value: str) -> str:
        if not _PITCH.fullmatch(value):
            raise ValueError("pitch must look like +0Hz or -50Hz")
        return value


def require_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("EDGE_TTS_INTERNAL_TOKEN", "")
    if expected and x_internal_token != expected:
        raise HTTPException(status_code=401, detail="invalid internal token")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "provider": "edge-tts"}


@app.get("/v1/voices", dependencies=[Depends(require_internal_token)])
async def voices() -> list[dict[str, object]]:
    return await edge_tts.list_voices()


@app.post("/v1/speech", dependencies=[Depends(require_internal_token)])
async def speech(request: SpeechRequest) -> StreamingResponse:
    async def stream_audio() -> AsyncIterator[bytes]:
        communicator = edge_tts.Communicate(
            text=request.text,
            voice=request.voice,
            rate=request.rate,
            volume=request.volume,
            pitch=request.pitch,
        )
        async for chunk in communicator.stream():
            if chunk["type"] == "audio":
                yield chunk["data"]

    return StreamingResponse(
        stream_audio(),
        media_type="audio/mpeg",
        headers={"Cache-Control": "private, no-store"},
    )

