import logging
import time
from pathlib import Path
from typing import cast

import demucs.api
import torch

logger = logging.getLogger(__name__)


class TrackSeparator:
    def __init__(self) -> None:
        logger.info("loading demucs separator")
        self._separator = demucs.api.Separator()
        logger.info(
            "demucs separator ready (samplerate=%s)", self._separator.samplerate
        )

    def separate_file(self, audio_file: Path) -> dict[str, torch.Tensor]:
        logger.info("separating file %s", audio_file)
        started = time.perf_counter()
        _origin, separated = self._separator.separate_audio_file(file=audio_file)
        stems = cast(dict[str, torch.Tensor], separated)
        logger.info(
            "separated %s into stems %s in %.1fs",
            audio_file,
            sorted(stems),
            time.perf_counter() - started,
        )
        return stems

    def separate_audio(
        self, tensor: torch.Tensor, sr: int | None = None
    ) -> dict[str, torch.Tensor]:
        logger.debug("separating tensor shape=%s sr=%s", tuple(tensor.shape), sr)
        started = time.perf_counter()
        _origin, stems = self._separator.separate_tensor(wav=tensor, sr=sr)
        logger.debug(
            "separated tensor into stems %s in %.1fs",
            sorted(stems),
            time.perf_counter() - started,
        )
        return stems

    @property
    def sample_rate(self) -> int:
        return cast(int, self._separator.samplerate)
