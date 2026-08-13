import logging
from typing import BinaryIO

import torchaudio

from debut.audio import TrackSeparator
from debut.transcription import Note, OfflineNotesTranscriber

logger = logging.getLogger(__name__)


class SongTranscriber:
    def __init__(self) -> None:
        self._notes_transcriber = OfflineNotesTranscriber()
        self._track_separator = TrackSeparator()

    def transcribe(self, audio: BinaryIO) -> list[Note]:
        wav, sr = torchaudio.load(audio)
        logger.debug("vocals stem shape=%s sr=%s", tuple(wav), sr)
        notes = self._notes_transcriber.transcribe(wav, sr)
        return notes
