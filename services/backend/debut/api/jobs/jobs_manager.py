import logging
import uuid
from collections.abc import Callable
from concurrent.futures import ThreadPoolExecutor
from threading import Lock

from debut.api.jobs import Job

logger = logging.getLogger(__name__)


class JobsManager:
    def __init__(self, max_workers: int = 1) -> None:
        self._executor = ThreadPoolExecutor(max_workers=max_workers)
        self._jobs: dict[str, Job] = {}
        self._lock = Lock()

    def queue(self, job_func: Callable[[], object]) -> Job:
        job = Job(id=uuid.uuid4().hex)
        with self._lock:
            self._jobs[job.id] = job
        logger.info("job %s queued", job.id)
        self._executor.submit(self._submit, job_func, job.id)
        return job

    def find_by_id(self, job_id: str) -> Job | None:
        with self._lock:
            return self._jobs.get(job_id)

    def _submit(self, job_func: Callable[[], object], job_id: str) -> None:
        try:
            with self._lock:
                self._jobs[job_id].running()
            logger.info("job %s running", job_id)
            result = job_func()
            with self._lock:
                self._jobs[job_id].finished(result)
            logger.info("job %s finished", job_id)
        except Exception as e:
            with self._lock:
                self._jobs[job_id].failed(str(e))
            logger.exception("job %s failed", job_id)
