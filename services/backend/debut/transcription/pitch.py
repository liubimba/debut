import math
from dataclasses import dataclass

_NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]


@dataclass(frozen=True)
class Pitch:
    midi: int
    freq_hz: float
    cents_offset: float
    confidence: float

    @classmethod
    def from_hz(cls, freq_hz: float, confidence: float) -> "Pitch":
        semitones = 69 + 12 * math.log2(freq_hz / 440.0)
        midi = round(semitones)
        return cls(midi, freq_hz, (semitones - midi) * 100, confidence)

    @property
    def name(self) -> str:
        return f"{_NOTE_NAMES[self.midi % 12]}{self.midi // 12 - 1}"
