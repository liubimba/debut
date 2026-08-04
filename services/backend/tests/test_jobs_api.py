from collections.abc import Iterator
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from debut.api.dependencies import get_jobs_manager, get_song_transcriber
from debut.api.jobs import Job, JobState
from debut.app import app


@pytest.fixture
def client() -> Iterator[TestClient]:
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_post_transcribe_queues_job_and_returns_202(client: TestClient) -> None:
    manager = MagicMock()
    manager.queue.return_value = Job(id="abc", state=JobState.QUEUED)
    app.dependency_overrides[get_song_transcriber] = lambda: MagicMock()
    app.dependency_overrides[get_jobs_manager] = lambda: manager

    response = client.post(
        "/api/v1/audio/transcribe",
        files={"file": ("song.mp3", BytesIO(b"audio-bytes"), "audio/mpeg")},
    )

    assert response.status_code == 202
    assert response.json()["id"] == "abc"
    manager.queue.assert_called_once()


def test_get_job_returns_state_and_id(client: TestClient) -> None:
    manager = MagicMock()
    manager.find_by_id.return_value = Job(id="abc", state=JobState.FINISHED, result=[])
    app.dependency_overrides[get_jobs_manager] = lambda: manager

    response = client.get("/api/v1/jobs/abc")

    assert response.status_code == 200
    body = response.json()
    assert body["id"] == "abc"
    assert body["state"] == "FINISHED"


def test_get_unknown_job_returns_404(client: TestClient) -> None:
    manager = MagicMock()
    manager.find_by_id.return_value = None
    app.dependency_overrides[get_jobs_manager] = lambda: manager

    response = client.get("/api/v1/jobs/missing")

    assert response.status_code == 404
