from typing import Annotated, cast

from fastapi import Depends
from starlette.requests import HTTPConnection

from debut.api.jobs.jobs_manager import JobsManager
from debut.audio import TrackSeparator
from debut.audio.stems_store import StemsStore
from debut.transcription.song_transcriber import SongTranscriber


def get_song_transcriber(request: HTTPConnection) -> SongTranscriber:
    return cast(SongTranscriber, request.app.state.song_transcriber)


def get_jobs_manager(request: HTTPConnection) -> JobsManager:
    return cast(JobsManager, request.app.state.jobs_manager)


def get_track_separator(request: HTTPConnection) -> TrackSeparator:
    return cast(TrackSeparator, request.app.state.track_separator)


def get_stems_store(request: HTTPConnection) -> StemsStore:
    return cast(StemsStore, request.app.state.stems_store)


SongTranscriberDep = Annotated[SongTranscriber, Depends(get_song_transcriber)]
JobsManagerDep = Annotated[JobsManager, Depends(get_jobs_manager)]
TrackSeparatorDep = Annotated[TrackSeparator, Depends(get_track_separator)]
StemsStoreDep = Annotated[StemsStore, Depends(get_stems_store)]
