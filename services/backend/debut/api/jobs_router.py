import logging

from fastapi import APIRouter, HTTPException

from debut.api.dependencies import JobsManagerDep
from debut.api.jobs import Job

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/jobs", tags=["jobs"])


@router.get("/{job_id}", response_model=Job)
def find_job_by_id(job_id: str, jobs_manager: JobsManagerDep) -> Job:
    logger.debug("status query for job %s", job_id)
    job = jobs_manager.find_by_id(job_id)
    if job is None:
        logger.info("job %s not found", job_id)
        raise HTTPException(status_code=404, detail="Job not found")
    return job
