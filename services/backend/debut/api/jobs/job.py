from dataclasses import dataclass, field
from datetime import datetime

from debut.api.jobs.job_state import JobState


@dataclass
class Job:
    id: str
    state: JobState = JobState.QUEUED
    started_at: datetime = field(default_factory=datetime.now)
    finished_at: datetime | None = None
    error_message: str | None = None
    result: object | None = None

    def running(self) -> None:
        self.state = JobState.RUNNING

    def finished(self, result: object | None = None) -> None:
        self.state = JobState.FINISHED
        self.finished_at = datetime.now()
        self.result = result

    def failed(self, reason: str | None = None) -> None:
        self.state = JobState.FAILED
        self.error_message = reason
