import pathlib

import pytest
import torch

from debut.audio.stems_store import StemsStore
from debut.path.app_path import AppPath


def _store(root: pathlib.Path) -> StemsStore:
    return StemsStore(app_path=AppPath(root=root))


def test_list_stems_returns_saved_filenames(tmp_path: pathlib.Path) -> None:
    stem_dir = tmp_path / "stems" / "job1"
    stem_dir.mkdir(parents=True)
    (stem_dir / "vocals.wav").touch()

    stems = _store(tmp_path).list_stems("job1")

    assert [stem.name for stem in stems] == ["vocals.wav"]


def test_list_stems_missing_returns_empty(tmp_path: pathlib.Path) -> None:
    assert _store(tmp_path).list_stems("nope") == []


def test_list_stems_rejects_path_traversal(tmp_path: pathlib.Path) -> None:
    with pytest.raises(ValueError, match="not relative"):
        _store(tmp_path).list_stems("../../etc")


def test_list_all_stems_completes_without_deadlock(tmp_path: pathlib.Path) -> None:
    stem_dir = tmp_path / "stems" / "job1"
    stem_dir.mkdir(parents=True)
    (stem_dir / "vocals.wav").touch()

    result = _store(tmp_path).list_all_stems()

    assert list(result.keys()) == ["job1"]
    assert [stem.name for stem in result["job1"]] == ["vocals.wav"]


@pytest.mark.slow
def test_save_writes_wav_files(tmp_path: pathlib.Path) -> None:
    saved = _store(tmp_path).save(
        {"vocals": torch.zeros(2, 100)}, sample_rate=44100, stem_id="job1"
    )

    assert [stem.name for stem in saved] == ["vocals.wav"]
    assert (tmp_path / "stems" / "job1" / "vocals.wav").exists()
