import os
import tempfile
from pathlib import Path
from typing import BinaryIO

from debut.audio import TrackSeparator
from debut.transcription import Note, OfflineNotesTranscriber


class SongTranscriber:
    def __init__(self) -> None:
        self._notes_transcriber = OfflineNotesTranscriber()
        self._track_separator = TrackSeparator()

    def transcribe(self, audio: BinaryIO) -> list[Note]:
        fd, name = tempfile.mkstemp(suffix=".audio")
        path = Path(name)
        try:
            with os.fdopen(fd, "wb") as file:
                file.write(audio.read())
            stems = self._track_separator.separate_file(path)
        finally:
            path.unlink(missing_ok=True)
        vocals = stems["vocals"]
        return self._notes_transcriber.transcribe(
            vocals, self._track_separator.sample_rate
        )
