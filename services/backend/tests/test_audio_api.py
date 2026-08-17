import pathlib
from collections.abc import Callable, Iterator
from io import BytesIO
from typing import Any
from unittest.mock import MagicMock

import pytest
import torch
from fastapi.testclient import TestClient

from debut.api.dependencies import (
    get_jobs_manager,
    get_stems_store,
    get_track_metadata_reader,
    get_track_separator,
)
from debut.api.jobs import Job, JobState
from debut.app import app
from debut.audio.stem import Stem
from debut.audio.track_metadata import TrackMetadataReader


@pytest.fixture
def client() -> Iterator[TestClient]:
    yield TestClient(app)
    app.dependency_overrides.clear()


def separate(
    client: TestClient,
    upload: bytes,
    stems: dict[str, torch.Tensor],
    sample_rate: int = 44100,
) -> dict[str, Any]:
    separator = MagicMock()
    separator.separate_bytes.return_value = stems
    store = MagicMock()
    store.save.return_value = [
        Stem(name=f"{name}.wav", path=pathlib.Path(f"/stems/{name}.wav"))
        for name in stems
    ]
    manager = MagicMock()
    manager.queue.return_value = Job(id="abc", state=JobState.QUEUED)
    app.dependency_overrides[get_track_separator] = lambda: separator
    app.dependency_overrides[get_stems_store] = lambda: store
    app.dependency_overrides[get_jobs_manager] = lambda: manager
    app.dependency_overrides[get_track_metadata_reader] = lambda: TrackMetadataReader()

    response = client.post(
        "/api/v1/audio/separate",
        files={"file": ("song.mp3", BytesIO(upload), "audio/mpeg")},
        data={"sample_rate": str(sample_rate)},
    )

    assert response.status_code == 202
    run = manager.queue.call_args.args[0]
    return dict(run().model_dump())


def test_separate_result_reports_duration_of_the_stems(client: TestClient) -> None:
    result = separate(
        client,
        upload=b"audio-bytes",
        stems={"vocals": torch.zeros(2, 88200)},
        sample_rate=44100,
    )

    assert result["duration_seconds"] == pytest.approx(2.0)


def test_separate_result_reports_tempo_of_the_drums(
    client: TestClient, click_track: Callable[..., tuple[torch.Tensor, int]]
) -> None:
    drums, sample_rate = click_track(bpm=120.0)

    result = separate(
        client,
        upload=b"audio-bytes",
        stems={"drums": drums, "vocals": torch.zeros_like(drums)},
        sample_rate=sample_rate,
    )

    assert result["tempo_bpm"] == pytest.approx(120.0, abs=2.0)


def test_separate_result_carries_tags_of_the_upload(
    client: TestClient, tagged_mp3: Callable[[str, str], bytes]
) -> None:
    result = separate(
        client,
        upload=tagged_mp3("Runaway", "Kanye West"),
        stems={"vocals": torch.zeros(2, 44100)},
    )

    assert result["title"] == "Runaway"
    assert result["artist"] == "Kanye West"


def test_separate_result_keeps_stem_id_and_names(client: TestClient) -> None:
    result = separate(
        client,
        upload=b"audio-bytes",
        stems={"vocals": torch.zeros(2, 44100)},
    )

    assert result["stems"] == ["vocals.wav"]
    assert result["stem_id"]
