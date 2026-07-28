import pathlib

import pytest
import torchaudio

from debut.export import notes_to_wav
from debut.transcription import Note, Pitch


def test_notes_render_to_a_normalized_playable_wav(tmp_path: pathlib.Path):
    notes = [
        Note(Pitch.from_hz(440.0, 0.9), 0.0, 0.5),
        Note(Pitch.from_hz(523.25, 0.8), 0.6, 1.2),
    ]
    out = tmp_path / "tones.wav"

    notes_to_wav(notes, out, sample_rate=22050)

    wav, sr = torchaudio.load(str(out))
    assert sr == 22050
    assert wav.shape[0] == 1
    assert wav.shape[1] / sr >= 1.2
    assert wav.abs().max().item() == pytest.approx(0.9, abs=0.01)
