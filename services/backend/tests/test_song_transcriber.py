import pathlib
from collections.abc import Callable
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
    stack.enter_context(patch("debut.transcription.song_transcriber.TrackSeparator"))
    load = stack.enter_context(patch("debut.transcription.song_transcriber.torchaudio"))
    transcriber = stack.enter_context(
        patch("debut.transcription.song_transcriber.OfflineNotesTranscriber")
    ).return_value
    return stack, load, transcriber


def test_decodes_audio_and_feeds_it_to_notes_transcriber() -> None:
    vocals = torch.zeros(2, 8)
    note = Note(Pitch.from_hz(440.0, 0.9), 0.0, 1.0)
    stack, torchaudio, transcriber = _mocked()
    with stack:
        torchaudio.load.return_value = (vocals, 32000)
        transcriber.transcribe.return_value = [note]
        audio = BytesIO(b"vocals-bytes")

        result = SongTranscriber().transcribe(audio)

    assert result == [note]
    assert torchaudio.load.call_args.args[0] is audio
    tensor_arg, sr_arg = transcriber.transcribe.call_args.args
    assert tensor_arg is vocals
    assert sr_arg == 32000


def test_does_not_separate_the_incoming_audio() -> None:
    stack, torchaudio, transcriber = _mocked()
    with stack:
        torchaudio.load.return_value = (torch.zeros(2, 8), 44100)
        transcriber.transcribe.return_value = []
        song_transcriber = SongTranscriber()
        separator = cast(MagicMock, song_transcriber._track_separator)

        song_transcriber.transcribe(BytesIO(b"vocals-bytes"))

        assert separator.separate_file.call_count == 0
        assert separator.separate_bytes.call_count == 0


def test_decoding_failure_propagates() -> None:
    stack, torchaudio, _transcriber = _mocked()
    with stack:
        torchaudio.load.side_effect = RuntimeError("broken audio")

        with pytest.raises(RuntimeError, match="broken audio"):
            SongTranscriber().transcribe(BytesIO(b"not-audio"))


@pytest.mark.slow
def test_real_vocals_yield_notes_in_vocal_range(
    sine_wav_file_path: Callable[..., pathlib.Path],
) -> None:
    path: pathlib.Path = sine_wav_file_path(freq_hz=220.0, seconds=1.0)

    with path.open("rb") as handle:
        notes = SongTranscriber().transcribe(handle)

    assert notes
    assert all(40 <= note.pitch.midi <= 84 for note in notes)
    assert all(note.duration > 0 for note in notes)
