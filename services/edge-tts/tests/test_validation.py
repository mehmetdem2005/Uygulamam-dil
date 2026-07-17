import pytest
from pydantic import ValidationError

from app.main import SpeechRequest


def test_accepts_supported_prosody() -> None:
    request = SpeechRequest(text="Hello", rate="-25%", pitch="+10Hz")
    assert request.text == "Hello"


def test_rejects_blank_text() -> None:
    with pytest.raises(ValidationError):
        SpeechRequest(text="   ")

