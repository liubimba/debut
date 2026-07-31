from datetime import datetime

from debut.api.jobs import Job, JobState
from debut.api.jobs.jobs_manager import JobsManager


def test_job_defaults_to_queued() -> None:
    job = Job(id="x")

    assert job.state is JobState.QUEUED
    assert job.result is None
    assert job.error_message is None
    assert isinstance(job.started_at, datetime)


def test_running_sets_state() -> None:
    job = Job(id="x")

    job.running()

    assert job.state is JobState.RUNNING


def test_finished_sets_state_result_and_timestamp() -> None:
    job = Job(id="x")

    job.finished(result=[1, 2, 3])

    assert job.state is JobState.FINISHED
    assert job.result == [1, 2, 3]
    assert job.finished_at is not None


def test_failed_sets_state_and_reason() -> None:
    job = Job(id="x")

    job.failed("boom")

    assert job.state is JobState.FAILED
    assert job.error_message == "boom"


def test_queue_runs_func_and_finishes_with_result() -> None:
    manager = JobsManager()

    job = manager.queue(lambda: "notes")
    manager._executor.shutdown(wait=True)

    assert job.state is JobState.FINISHED
    assert job.result == "notes"
    assert job.error_message is None


def test_queue_marks_job_failed_when_func_raises() -> None:
    manager = JobsManager()

    def boom() -> object:
        raise RuntimeError("kaboom")

    job = manager.queue(boom)
    manager._executor.shutdown(wait=True)

    assert job.state is JobState.FAILED
    assert job.error_message == "kaboom"


def test_find_by_id_returns_job_or_none() -> None:
    manager = JobsManager()

    job = manager.queue(lambda: None)
    manager._executor.shutdown(wait=True)

    assert manager.find_by_id(job.id) is job
    assert manager.find_by_id("missing") is None
