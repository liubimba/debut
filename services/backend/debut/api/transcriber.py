import logging
from io import BytesIO

from fastapi import APIRouter, UploadFile
from starlette import status

from debut.api.dependencies import JobsManagerDep, SongTranscriberDep
from debut.api.jobs import Job

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/transcriber", tags=["transcriber"])


@router.post("/transcribe", status_code=status.HTTP_202_ACCEPTED)
async def transcribe(
    song_transcriber: SongTranscriberDep,
    jobs_manager: JobsManagerDep,
    file: UploadFile,
) -> Job:
    data = await file.read()
    logger.info("received upload %s (%s bytes)", file.filename, len(data))
    job = jobs_manager.queue(lambda: song_transcriber.transcribe(BytesIO(data)))
    logger.info("queued transcription job %s", job.id)
    return job
