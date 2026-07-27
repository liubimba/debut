import pathlib

import torch

from separator import PitchDetector, TrackSeparator


class OfflinePitchAnalyzer:
    def __init__(self, pitch_detector: PitchDetector, track_separator: TrackSeparator):
        self._pitch_detector = pitch_detector
        self._track_separator = track_separator

    def analyze_audio(self, tensor: torch.Tensor, sr: int | None = None) -> tuple[torch.Tensor, torch.Tensor]:
        stems = self._track_separator.separate_audio(tensor, sr)
        return self._detect(stems)

    def analyze_file(self, audio_file: pathlib.Path) -> tuple[torch.Tensor, torch.Tensor]:
        stems = self._track_separator.separate_file(audio_file)
        return self._detect(stems)

    def _detect(self, stems: dict[str, torch.Tensor]) -> tuple[torch.Tensor, torch.Tensor]:
        vocals = stems["vocals"]
        return self._pitch_detector.detect_pitch(vocals, self._track_separator.get_sample_rate())


class StreamingPitchAnalyzer:
    def __init__(self, pitch_detector: PitchDetector,
                 window_sec: float = 0.20,
                 sr: int = 44100):
        self._pitch_detector = pitch_detector
        self._buffer = torch.zeros(0)
        self._window_sec = int(sr * window_sec)
        self._sr = sr

    def push_chunk(self, chunk: torch.Tensor) -> tuple[float, float]:
        mono = chunk if chunk.dim() == 1 else chunk.mean(dim=0)
        self._buffer = torch.cat([self._buffer, mono])[-self._window_sec:]
        f0, periodicity = self._pitch_detector.detect_pitch(self._buffer.unsqueeze(0), self._sr)
        return f0[0, -1], periodicity[0, -1].item()

    def reset(self) -> None:
        self._buffer = torch.zeros(0)
