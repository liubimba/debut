import pathlib

import pytest
import torch
import torchaudio


@pytest.fixture
def sine_stereo():
    def _make(freq_hz=440.0, seconds=2.0, sr=44100):
        t = torch.arange(int(sr * seconds)) / sr
        mono = 0.5 * torch.sin(2 * torch.pi * freq_hz * t)
        return torch.stack([mono, mono]), sr

    return _make


@pytest.fixture
def sine_wav_file_path(tmp_path: pathlib.Path):
    def _make(freq_hz=440.0, seconds=2.0, sr=44100):
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
