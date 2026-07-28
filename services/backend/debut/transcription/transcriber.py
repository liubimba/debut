import torch
from torch import Tensor

from debut.audio.pitch_detector import PitchDetector
from debut.transcription.note import Note
from debut.transcription.pitch import Pitch


class OfflineNotesTranscriber:
    def __init__(
        self,
        threshold: float = 0.5,
        min_duration: float = 0.06,
        max_gap: float = 0.05,
    ) -> None:
        self._pitch_detector = PitchDetector()
        self._threshold = threshold
        self._min_duration = min_duration
        self._max_gap = max_gap

    def transcribe(self, tensor: Tensor, sr: int) -> list[Note]:
        f0, periodicity = self._pitch_detector.detect_pitch(tensor=tensor, sr=sr)
        return self._clean(self._group(f0, periodicity, self._pitch_detector.frame_dt))

    def _group(self, f0: Tensor, periodicity: Tensor, frame_dt: float) -> list[Note]:
        notes: list[Note] = []
        midi: int | None = None
        acc: list[Pitch] = []
        start = 0.0

        def flush(end_time: float) -> None:
            nonlocal midi, acc
            if midi is not None and acc:
                notes.append(Note(self._aggregate(acc), start, end_time))
            midi, acc = None, []

        frames = f0.shape[1]
        for i in range(frames):
            confidence = periodicity[0, i].item()
            hz = f0[0, i].item()
            time = i * frame_dt
            if confidence < self._threshold or hz <= 0:
                flush(time)
                continue
            pitch = Pitch.from_hz(hz, confidence)
            if midi is None:
                midi, start, acc = pitch.midi, time, [pitch]
            elif pitch.midi == midi:
                acc.append(pitch)
            else:
                flush(time)
                midi, start, acc = pitch.midi, time, [pitch]
        flush(frames * frame_dt)
        return notes

    def _clean(self, notes: list[Note]) -> list[Note]:
        kept = [note for note in notes if note.duration >= self._min_duration]
        if not kept:
            return []
        merged = [kept[0]]
        for note in kept[1:]:
            previous = merged[-1]
            gap = note.start_time - previous.end_time
            if note.pitch.midi == previous.pitch.midi and gap <= self._max_gap:
                merged[-1] = Note(
                    self._aggregate([previous.pitch, note.pitch]),
                    previous.start_time,
                    note.end_time,
                )
            else:
                merged.append(note)
        return merged

    @staticmethod
    def _aggregate(pitches: list[Pitch]) -> Pitch:
        sorted_hz = sorted(pitch.freq_hz for pitch in pitches)
        return Pitch(
            midi=pitches[0].midi,
            freq_hz=sorted_hz[len(sorted_hz) // 2],
            cents_offset=sum(pitch.cents_offset for pitch in pitches) / len(pitches),
            confidence=sum(pitch.confidence for pitch in pitches) / len(pitches),
        )


class StreamingNoteTranscriber:
    def __init__(
        self,
        window_sec: float = 0.2,
        sample_rate: int = 44100,
        threshold: float = 0.5,
    ) -> None:
        self._window_samples = int(sample_rate * window_sec)
        self._sample_rate = sample_rate
        self._threshold = threshold
        self._pitch_detector = PitchDetector()
        self._buffer = torch.zeros(0)

    def transcribe(self, chunk: Tensor) -> Pitch | None:
        mono = chunk if chunk.dim() == 1 else chunk.mean(dim=0)
        self._buffer = torch.cat([self._buffer, mono])[-self._window_samples :]
        f0, periodicity = self._pitch_detector.detect_pitch(
            tensor=self._buffer, sr=self._sample_rate
        )
        confidence = periodicity[0, -1].item()
        hz = f0[0, -1].item()
        if confidence < self._threshold or hz <= 0:
            return None
        return Pitch.from_hz(hz, confidence)
