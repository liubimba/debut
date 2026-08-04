import pathlib
from collections.abc import Iterator
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from debut.api.dependencies import get_stems_store
from debut.app import app
from debut.audio.stem import Stem


@pytest.fixture
def client() -> Iterator[TestClient]:
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_list_stems_returns_download_urls_not_paths(client: TestClient) -> None:
    store = MagicMock()
    store.list_stems.return_value = [
        Stem(name="vocals.wav", path=pathlib.Path("/secret/vocals.wav"))
    ]
    app.dependency_overrides[get_stems_store] = lambda: store

    response = client.get("/api/v1/stems/job1")

    assert response.status_code == 200
    body = response.json()
    assert body[0]["name"] == "vocals.wav"
    assert body[0]["url"].endswith("/api/v1/stems/job1/vocals.wav")
    assert "/secret/" not in body[0]["url"]


def test_traversal_error_maps_to_400(client: TestClient) -> None:
    store = MagicMock()
    store.list_stems.side_effect = ValueError("not relative")
    app.dependency_overrides[get_stems_store] = lambda: store

    response = client.get("/api/v1/stems/whatever")

    assert response.status_code == 400


def test_missing_stem_returns_404(client: TestClient) -> None:
    store = MagicMock()
    store.list_stems.return_value = []
    app.dependency_overrides[get_stems_store] = lambda: store

    response = client.get("/api/v1/stems/job1/vocals.wav")

    assert response.status_code == 404
