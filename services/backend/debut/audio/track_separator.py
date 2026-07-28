from pathlib import Path

import demucs.api
import torch


class TrackSeparator:
    def __init__(self) -> None:
        self._separator = demucs.api.Separator()

    def separate_file(self, audio_file: Path) -> dict[str, torch.Tensor]:
        _origin, separated = self._separator.separate_audio_file(file=audio_file)
        return separated

    def separate_audio(
        self, tensor: torch.Tensor, sr: int | None = None
    ) -> dict[str, torch.Tensor]:
        _origin, stems = self._separator.separate_tensor(wav=tensor, sr=sr)
        return stems

    @property
    def sample_rate(self) -> int:
        return self._separator.samplerate
