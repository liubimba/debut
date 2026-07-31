from unittest.mock import patch

import pytest
import torch

from debut.transcription import (
    Note,
    OfflineNotesTranscriber,
    StreamingNoteTranscriber,
)


def _frames(
    midi_values: list[int], periodicity: float = 0.9
) -> tuple[torch.Tensor, torch.Tensor]:
    hz = [440.0 * 2 ** ((m - 69) / 12) for m in midi_values]
    return torch.tensor([hz]), torch.full((1, len(hz)), float(periodicity))


def _transcribe(
    f0: torch.Tensor,
    periodicity: torch.Tensor,
    frame_dt: float = 0.01,
    **kwargs: float,
) -> list[Note]:
    with patch("debut.transcription.transcriber.PitchDetector") as detector_cls:
        detector = detector_cls.return_value
        detector.detect_pitch.return_value = (f0, periodicity)
        detector.frame_dt = frame_dt
        return OfflineNotesTranscriber(**kwargs).transcribe(torch.zeros(2, 4), sr=44100)


def test_steady_pitch_becomes_one_note() -> None:
    f0, per = _frames([69] * 100)

    notes = _transcribe(f0, per)

    assert len(notes) == 1
    assert notes[0].pitch.midi == 69
    assert notes[0].pitch.name == "A4"
    assert notes[0].duration == pytest.approx(1.0, abs=0.02)


def test_single_frame_glitch_is_absorbed() -> None:
    sequence = [69] * 50
    sequence[25] = 70
    f0, per = _frames(sequence)

    notes = _transcribe(f0, per, min_duration=0.05)

    assert [note.pitch.midi for note in notes] == [69]


def test_two_distinct_notes_are_split_at_the_boundary() -> None:
    f0, per = _frames([60] * 40 + [64] * 40)

    notes = _transcribe(f0, per)

    assert [note.pitch.midi for note in notes] == [60, 64]
    assert notes[0].end_time == pytest.approx(notes[1].start_time)


def test_silence_breaks_a_note_in_two() -> None:
    f0, per = _frames([62] * 60)
    per[0, 28:33] = 0.1

    notes = _transcribe(f0, per, max_gap=0.02)

    assert len(notes) == 2


def test_streaming_returns_current_pitch() -> None:
    f0 = torch.tensor([[440.0, 440.0, 440.0]])
    per = torch.tensor([[0.9, 0.9, 0.9]])
    with patch("debut.transcription.transcriber.PitchDetector") as detector_cls:
        detector_cls.return_value.detect_pitch.return_value = (f0, per)

        result = StreamingNoteTranscriber().transcribe(torch.zeros(2, 100))

    assert result is not None
    assert result.midi == 69
    assert result.confidence == pytest.approx(0.9)


def test_streaming_returns_none_when_unvoiced() -> None:
    f0 = torch.tensor([[440.0]])
    per = torch.tensor([[0.1]])
    with patch("debut.transcription.transcriber.PitchDetector") as detector_cls:
        detector_cls.return_value.detect_pitch.return_value = (f0, per)

        result = StreamingNoteTranscriber().transcribe(torch.zeros(2, 100))

    assert result is None
