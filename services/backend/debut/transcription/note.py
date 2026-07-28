from dataclasses import dataclass

from debut.transcription.pitch import Pitch


@dataclass
class Note:
    pitch: Pitch
    start_time: float
    end_time: float

    @property
    def duration(self) -> float:
        return self.end_time - self.start_time
