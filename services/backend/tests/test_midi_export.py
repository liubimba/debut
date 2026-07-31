import pathlib
from typing import cast

import pretty_midi
import pytest

from debut.export import notes_to_midi
from debut.transcription import Note, Pitch


def _read_back(path: pathlib.Path) -> list[pretty_midi.Note]:
    return cast(
        "list[pretty_midi.Note]",
        pretty_midi.PrettyMIDI(str(path)).instruments[0].notes,
    )


def test_notes_round_trip_through_midi(tmp_path: pathlib.Path) -> None:
    notes = [
        Note(Pitch.from_hz(440.0, 0.9), 0.0, 0.5),
        Note(Pitch.from_hz(523.25, 0.8), 0.6, 1.2),
    ]
    out = tmp_path / "out.mid"

    notes_to_midi(notes, out)

    played = _read_back(out)
    assert [note.pitch for note in played] == [69, 72]
    assert played[0].start == pytest.approx(0.0, abs=0.01)
    assert played[0].end == pytest.approx(0.5, abs=0.01)
    assert played[1].start == pytest.approx(0.6, abs=0.01)
    assert played[1].end == pytest.approx(1.2, abs=0.01)


def test_velocity_scales_with_confidence(tmp_path: pathlib.Path) -> None:
    notes = [
        Note(Pitch.from_hz(440.0, 1.0), 0.0, 0.5),
        Note(Pitch.from_hz(440.0, 0.1), 0.6, 1.0),
    ]
    out = tmp_path / "velocity.mid"

    notes_to_midi(notes, out)

    played = _read_back(out)
    assert played[0].velocity > played[1].velocity


def test_empty_notes_write_a_valid_file_with_no_notes(tmp_path: pathlib.Path) -> None:
    out = tmp_path / "empty.mid"

    notes_to_midi([], out)

    restored = pretty_midi.PrettyMIDI(str(out))
    assert sum(len(instrument.notes) for instrument in restored.instruments) == 0
