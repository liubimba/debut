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
    TrackMetadataReaderDep,
    TrackSeparatorDep,
)
from debut.api.jobs import Job

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/audio", tags=["audio"])


class AudioSeparateJobResponse(BaseModel):
    stem_id: str
    stems: list[str]
    duration_seconds: float
    tempo_bpm: float | None
    title: str | None
    artist: str | None


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
    metadata_reader: TrackMetadataReaderDep,
    file: UploadFile,
    sample_rate: int = Form(44100),
    stem_id: str | None = Form(None),
) -> Job:
    data = await file.read()
    logger.info("received upload %s (%s bytes)", file.filename, len(data))
    sid = stem_id or uuid.uuid4().hex

    def run() -> AudioSeparateJobResponse:
        stems = track_separator.separate_bytes(data)
        saved = stems_store.save(stems, sample_rate=sample_rate, stem_id=sid)
        metadata = metadata_reader.read(data=data, stems=stems, sample_rate=sample_rate)
        return AudioSeparateJobResponse(
            stem_id=sid,
            stems=[stem.name for stem in saved],
            duration_seconds=metadata.duration_seconds,
            tempo_bpm=metadata.tempo_bpm,
            title=metadata.title,
            artist=metadata.artist,
        )

    job = jobs_manager.queue(run)
    logger.info("queued separation job %s", job.id)
    return job
