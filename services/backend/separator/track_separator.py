from pathlib import Path

import demucs.api
import torch


class TrackSeparator:
    def __init__(self, sample_rate: int = 44100):
        self._separator = demucs.api.Separator()
        self._sample_rate = sample_rate

    def separate_file(self, audio_file: Path) -> dict[str, torch.Tensor]:
        _origin, separated = self._separator.separate_audio_file(file=audio_file)
        return separated

    def separate_audio(self, tensor: torch.Tensor, sr: int | None = None) ->  dict[str, torch.Tensor]:
        _origin, stems = self._separator.separate_tensor(wav=tensor, sr=sr)
        return stems

    def get_sample_rate(self) -> int:
        return self._sample_rate
