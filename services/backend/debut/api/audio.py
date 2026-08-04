import logging
import uuid
from io import BytesIO

from fastapi import APIRouter, Form, UploadFile
from pydantic import BaseModel
from starlette import status

from debut.api.dependencies import (
    JobsManagerDep,
    SongTranscriberDep,
    StemsStoreDep,
    TrackSeparatorDep,
)
from debut.api.jobs import Job

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/audio", tags=["audio"])


class AudioSeparateJobResponse(BaseModel):
    stem_id: str
    stems: list[str]


@router.post("/transcribe", status_code=status.HTTP_202_ACCEPTED)
async def audio_transcribe(
    song_transcriber: SongTranscriberDep,
    jobs_manager: JobsManagerDep,
    file: UploadFile,
) -> Job:
    data = await file.read()
    logger.info("received upload %s (%s bytes)", file.filename, len(data))
    job = jobs_manager.queue(lambda: song_transcriber.transcribe(BytesIO(data)))
    logger.info("queued transcription job %s", job.id)
    return job


@router.post("/separate", status_code=status.HTTP_202_ACCEPTED)
async def audio_separate(
    track_separator: TrackSeparatorDep,
    jobs_manager: JobsManagerDep,
    stems_store: StemsStoreDep,
    file: UploadFile,
    sample_rate: int = Form(44100),
    stem_id: str | None = Form(None),
) -> Job:
    data = await file.read()
    logger.info("received upload %s (%s bytes)", file.filename, len(data))
    sid = stem_id or uuid.uuid4().hex

    def run() -> AudioSeparateJobResponse:
        saved = stems_store.save(
            track_separator.separate_bytes(data), sample_rate=sample_rate, stem_id=sid
        )
        return AudioSeparateJobResponse(
            stem_id=sid, stems=[stem.name for stem in saved]
        )

    job = jobs_manager.queue(run)
    logger.info("queued separation job %s", job.id)
    return job
