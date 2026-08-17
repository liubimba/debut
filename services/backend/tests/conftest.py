import pathlib
from collections.abc import Callable

import mutagen
import pytest
import torch
import torchaudio


@pytest.fixture
def sine_stereo() -> Callable[..., tuple[torch.Tensor, int]]:
    def _make(
        freq_hz: float = 440.0, seconds: float = 2.0, sr: int = 44100
    ) -> tuple[torch.Tensor, int]:
        t = torch.arange(int(sr * seconds)) / sr
        mono = 0.5 * torch.sin(2 * torch.pi * freq_hz * t)
        return torch.stack([mono, mono]), sr

    return _make


@pytest.fixture
def sine_wav_file_path(
    tmp_path: pathlib.Path,
) -> Callable[..., pathlib.Path]:
    def _make(
        freq_hz: float = 440.0, seconds: float = 2.0, sr: int = 44100
    ) -> pathlib.Path:
        t = torch.arange(int(sr * seconds)) / sr
        mono = 0.5 * torch.sin(2 * torch.pi * t * freq_hz)
        stereo = torch.stack([mono, mono])
        path = tmp_path / "sine.wav"
        torchaudio.save(str(path), stereo, sr)
        return path

    return _make


@pytest.fixture
def data_dir() -> pathlib.Path:
    return pathlib.Path(__file__).parent / "data"


@pytest.fixture
def click_track() -> Callable[..., tuple[torch.Tensor, int]]:
    def _make(
        bpm: float = 120.0, seconds: float = 12.0, sr: int = 22050
    ) -> tuple[torch.Tensor, int]:
        torch.manual_seed(0)
        samples = int(sr * seconds)
        signal = torch.zeros(samples)
        envelope = torch.exp(-torch.arange(int(sr * 0.02)) / (sr * 0.004))
        click = (torch.rand(envelope.shape[0]) * 2 - 1) * envelope
        period = int(sr * 60.0 / bpm)
        for start in range(0, samples - click.shape[0], period):
            signal[start : start + click.shape[0]] += click
        return torch.stack([signal, signal]), sr

    return _make


@pytest.fixture
def tagged_mp3(
    tmp_path: pathlib.Path, data_dir: pathlib.Path
) -> Callable[[str, str], bytes]:
    def _make(title: str, artist: str) -> bytes:
        path = tmp_path / "tagged.mp3"
        path.write_bytes((data_dir / "runaway.mp3").read_bytes())
        audio = mutagen.File(path, easy=True)
        assert audio is not None
        audio.add_tags()
        audio["title"] = title
        audio["artist"] = artist
        audio.save()
        return path.read_bytes()

    return _make
