import logging

from debut.api.jobs.jobs_manager import JobsManager
from debut.core.context import AppContext
from debut.transcription.song_transcriber import SongTranscriber

logger = logging.getLogger(__name__)


class BackendBuilder:
    def __init__(self) -> None:
        pass

    async def build(self) -> AppContext:
        logger.info("creating SongTranscriber (loads demucs + crepe)")
        song_transcriber = SongTranscriber()
        jobs_manager = JobsManager()
        logger.info("backend context built")
        return AppContext(
            song_transcriber=song_transcriber,
            jobs_manager=jobs_manager,
        )
