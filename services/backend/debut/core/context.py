from dataclasses import dataclass

from debut.api.jobs.jobs_manager import JobsManager
from debut.transcription.song_transcriber import SongTranscriber


@dataclass(frozen=True)
class AppContext:
    song_transcriber: SongTranscriber
    jobs_manager: JobsManager
