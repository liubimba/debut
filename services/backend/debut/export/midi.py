from pathlib import Path

import pretty_midi

from debut.transcription import Note


def notes_to_midi(notes: list[Note], path: Path) -> None:
    build_midi(notes).write(str(path))


def build_midi(notes: list[Note]) -> pretty_midi.PrettyMIDI:
    midi = pretty_midi.PrettyMIDI()
    instrument = pretty_midi.Instrument(program=0)
    for note in notes:
        instrument.notes.append(
            pretty_midi.Note(
                velocity=_velocity(note.pitch.confidence),
                pitch=note.pitch.midi,
                start=note.start_time,
                end=note.end_time,
            )
        )
    midi.instruments.append(instrument)
    return midi


def _velocity(confidence: float) -> int:
    return max(1, min(127, round(confidence * 127)))
