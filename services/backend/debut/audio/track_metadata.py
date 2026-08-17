import logging
import math
from dataclasses import dataclass
from io import BytesIO

import librosa
import mutagen
import numpy as np
import torch

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class TrackTags:
    title: str | None = None
    artist: str | None = None


@dataclass(frozen=True)
class TrackMetadata:
    title: str | None
    artist: str | None
    duration_seconds: float
    tempo_bpm: float | None


class TrackMetadataReader:
    def __init__(
        self,
        percussive_stem: str = "drums",
        silence_threshold: float = 1e-6,
        minimum_beats: int = 3,
        outlier_tolerance: float = 0.25,
    ) -> None:
        self._percussive_stem = percussive_stem
        self._silence_threshold = silence_threshold
        self._minimum_beats = minimum_beats
        self._outlier_tolerance = outlier_tolerance

    def read(
        self, data: bytes, stems: dict[str, torch.Tensor], sample_rate: int
    ) -> TrackMetadata:
        tags = self.read_tags(data)
        percussive = stems.get(self._percussive_stem)
        if percussive is None:
            logger.info("no %s stem, tempo left unknown", self._percussive_stem)
            tempo = None
        else:
            tempo = self.estimate_tempo(percussive, sample_rate)
        return TrackMetadata(
            title=tags.title,
            artist=tags.artist,
            duration_seconds=self.measure_duration(stems, sample_rate),
            tempo_bpm=tempo,
        )

    def read_tags(self, data: bytes) -> TrackTags:
        try:
            audio = mutagen.File(BytesIO(data), easy=True)
        except mutagen.MutagenError:
            logger.info("no readable tags in upload of %s bytes", len(data))
            return TrackTags()
        if audio is None or audio.tags is None:
            logger.info("upload of %s bytes carries no tags", len(data))
            return TrackTags()
        tags = TrackTags(
            title=self._first(audio.tags.get("title")),
            artist=self._first(audio.tags.get("artist")),
        )
        logger.info("read tags %s", tags)
        return tags

    def measure_duration(
        self, stems: dict[str, torch.Tensor], sample_rate: int
    ) -> float:
        frames = max((stem.shape[-1] for stem in stems.values()), default=0)
        return frames / sample_rate

    def estimate_tempo(self, tensor: torch.Tensor, sample_rate: int) -> float | None:
        mono = tensor.mean(dim=0) if tensor.dim() > 1 else tensor
        samples = mono.detach().cpu().numpy().astype(np.float32)
        onset_envelope = librosa.onset.onset_strength(y=samples, sr=sample_rate)
        if (
            onset_envelope.size == 0
            or float(np.max(onset_envelope)) <= self._silence_threshold
        ):
            logger.info("no onsets in %s samples, tempo unknown", samples.size)
            return None
        tempo = self._tempo_from_beats(onset_envelope, sample_rate)
        if tempo is None:
            tempo = float(
                librosa.feature.tempo(onset_envelope=onset_envelope, sr=sample_rate)[0]
            )
        if not math.isfinite(tempo) or tempo <= 0:
            logger.info("tempo estimate %s is not usable", tempo)
            return None
        logger.info("estimated tempo %.1f bpm", tempo)
        return tempo

    def _tempo_from_beats(
        self, onset_envelope: np.typing.NDArray[np.float32], sample_rate: int
    ) -> float | None:
        _static, beats = librosa.beat.beat_track(
            onset_envelope=onset_envelope, sr=sample_rate, units="time"
        )
        if beats.size < self._minimum_beats:
            return None
        intervals = np.diff(beats)
        median = float(np.median(intervals))
        if median <= 0:
            return None
        regular = intervals[
            np.abs(intervals - median) <= self._outlier_tolerance * median
        ]
        interval = float(np.mean(regular)) if regular.size else median
        return 60.0 / interval

    @staticmethod
    def _first(values: object) -> str | None:
        if isinstance(values, list) and values:
            first = values[0]
            return first if isinstance(first, str) else str(first)
        return None
