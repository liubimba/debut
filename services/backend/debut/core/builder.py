import logging

from debut.api.jobs.jobs_manager import JobsManager
from debut.audio import TrackSeparator
from debut.audio.stems_store import StemsStore
from debut.audio.track_metadata import TrackMetadataReader
from debut.core.context import AppContext
from debut.path.app_path import AppPath
from debut.transcription.song_transcriber import SongTranscriber

logger = logging.getLogger(__name__)


class BackendBuilder:
    def __init__(self) -> None:
        pass

    async def build(self) -> AppContext:
        app_path = AppPath()
        logger.info("creating SongTranscriber (loads demucs + crepe)")
        song_transcriber = SongTranscriber()
        jobs_manager = JobsManager()
        track_separator = TrackSeparator()
        logger.info("backend context built")
        stems_store = StemsStore(app_path=app_path)
        track_metadata_reader = TrackMetadataReader()
        return AppContext(
            song_transcriber=song_transcriber,
            jobs_manager=jobs_manager,
            track_separator=track_separator,
            stems_store=stems_store,
            track_metadata_reader=track_metadata_reader,
        )
