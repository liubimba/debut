import pathlib
from contextlib import ExitStack
from io import BytesIO
from typing import cast
from unittest.mock import MagicMock, patch

import pytest
import torch

from debut.transcription import Note, Pitch
from debut.transcription.song_transcriber import SongTranscriber


def _mocked() -> tuple[ExitStack, MagicMock, MagicMock]:
    stack = ExitStack()
    separator = stack.enter_context(
        patch("debut.transcription.song_transcriber.TrackSeparator")
    ).return_value
    transcriber = stack.enter_context(
        patch("debut.transcription.song_transcriber.OfflineNotesTranscriber")
    ).return_value
    return stack, separator, transcriber


def test_feeds_vocals_stem_and_separator_rate_to_transcriber() -> None:
    vocals = torch.zeros(2, 8)
    note = Note(Pitch.from_hz(440.0, 0.9), 0.0, 1.0)
    stack, separator, transcriber = _mocked()
    with stack:
        separator.separate_file.return_value = {
            "vocals": vocals,
            "drums": torch.zeros(2, 8),
        }
        separator.sample_rate = 32000
        transcriber.transcribe.return_value = [note]

        result = SongTranscriber().transcribe(BytesIO(b"audio-bytes"))

    assert result == [note]
    audio_arg, sr_arg = transcriber.transcribe.call_args.args
    assert audio_arg is vocals
    assert sr_arg == 32000


def test_writes_uploaded_bytes_to_a_file_then_removes_it() -> None:
    seen: dict[str, object] = {}

    def fake_separate(audio_file: pathlib.Path) -> dict[str, torch.Tensor]:
        seen["path"] = audio_file
        seen["present_during_separation"] = audio_file.exists()
        seen["content"] = audio_file.read_bytes()
        return {"vocals": torch.zeros(2, 8)}

    stack, separator, transcriber = _mocked()
    with stack:
        separator.separate_file.side_effect = fake_separate
        separator.sample_rate = 44100
        transcriber.transcribe.return_value = []

        SongTranscriber().transcribe(BytesIO(b"payload"))

    assert seen["present_during_separation"] is True
    assert seen["content"] == b"payload"
    assert not cast(pathlib.Path, seen["path"]).exists()


def test_temp_file_removed_even_when_separation_fails() -> None:
    seen: dict[str, pathlib.Path] = {}

    def boom(audio_file: pathlib.Path) -> dict[str, torch.Tensor]:
        seen["path"] = audio_file
        raise RuntimeError("separation failed")

    stack, separator, _transcriber = _mocked()
    with stack:
        separator.separate_file.side_effect = boom

        with pytest.raises(RuntimeError):
            SongTranscriber().transcribe(BytesIO(b"payload"))

    assert not seen["path"].exists()


@pytest.mark.slow
def test_real_song_yields_notes_in_vocal_range(data_dir: pathlib.Path) -> None:
    song = data_dir / "runaway.mp3"

    with song.open("rb") as handle:
        notes = SongTranscriber().transcribe(handle)

    assert notes
    assert all(40 <= note.pitch.midi <= 84 for note in notes)
    assert all(note.duration > 0 for note in notes)
