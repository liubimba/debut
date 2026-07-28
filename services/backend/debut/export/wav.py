from pathlib import Path

import torch
import torchaudio

from debut.export.midi import build_midi
from debut.transcription import Note


def notes_to_wav(notes: list[Note], path: Path, sample_rate: int = 44100) -> None:
    audio = build_midi(notes).synthesize(fs=sample_rate)
    wav = torch.tensor(audio, dtype=torch.float32).unsqueeze(0)
    peak = wav.abs().max()
    if peak > 0:
        wav = wav / peak * 0.9
    torchaudio.save(str(path), wav, sample_rate)
