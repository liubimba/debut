import logging
import os
import tempfile
import time
from pathlib import Path
from typing import BinaryIO

from debut.audio import TrackSeparator
from debut.transcription import Note, OfflineNotesTranscriber

logger = logging.getLogger(__name__)


class SongTranscriber:
    def __init__(self) -> None:
        self._notes_transcriber = OfflineNotesTranscriber()
        self._track_separator = TrackSeparator()

    def transcribe(self, audio: BinaryIO) -> list[Note]:
        started = time.perf_counter()
        fd, name = tempfile.mkstemp(suffix=".audio")
        path = Path(name)
        try:
            with os.fdopen(fd, "wb") as file:
                written = file.write(audio.read())
            logger.info("song transcription started (%s bytes)", written)
            stems = self._track_separator.separate_file(path)
        finally:
            path.unlink(missing_ok=True)
        vocals = stems["vocals"]
        sample_rate = self._track_separator.sample_rate
        logger.debug("vocals stem shape=%s sr=%s", tuple(vocals.shape), sample_rate)
        notes = self._notes_transcriber.transcribe(vocals, sample_rate)
        logger.info(
            "song transcription finished: %s notes in %.1fs",
            len(notes),
            time.perf_counter() - started,
        )
        return notes
