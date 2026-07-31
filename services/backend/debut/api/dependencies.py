from typing import Annotated, cast

from fastapi import Depends
from starlette.requests import HTTPConnection

from debut.api.jobs.jobs_manager import JobsManager
from debut.transcription.song_transcriber import SongTranscriber


def get_song_transcriber(request: HTTPConnection) -> SongTranscriber:
    return cast(SongTranscriber, request.app.state.song_transcriber)


def get_jobs_manager(request: HTTPConnection) -> JobsManager:
    return cast(JobsManager, request.app.state.jobs_manager)


SongTranscriberDep = Annotated[SongTranscriber, Depends(get_song_transcriber)]
JobsManagerDep = Annotated[JobsManager, Depends(get_jobs_manager)]
