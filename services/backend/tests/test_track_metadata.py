import pathlib
from collections.abc import Callable

import pytest
import torch

from debut.audio.track_metadata import TrackMetadataReader


def test_reads_title_and_artist_from_id3(
    tagged_mp3: Callable[[str, str], bytes],
) -> None:
    tags = TrackMetadataReader().read_tags(tagged_mp3("Runaway", "Kanye West"))

    assert tags.title == "Runaway"
    assert tags.artist == "Kanye West"


def test_untagged_file_yields_empty_tags(data_dir: pathlib.Path) -> None:
    tags = TrackMetadataReader().read_tags((data_dir / "runaway.mp3").read_bytes())

    assert tags.title is None
    assert tags.artist is None


def test_unparsable_bytes_yield_empty_tags() -> None:
    tags = TrackMetadataReader().read_tags(b"this is not audio at all")

    assert tags.title is None
    assert tags.artist is None


def test_estimates_tempo_of_a_stereo_click_track(
    click_track: Callable[..., tuple[torch.Tensor, int]],
) -> None:
    tensor, sample_rate = click_track(bpm=120.0)

    tempo = TrackMetadataReader().estimate_tempo(tensor, sample_rate)

    assert tempo == pytest.approx(120.0, abs=2.0)


def test_follows_a_slower_click_track(
    click_track: Callable[..., tuple[torch.Tensor, int]],
) -> None:
    tensor, sample_rate = click_track(bpm=90.0)

    tempo = TrackMetadataReader().estimate_tempo(tensor, sample_rate)

    assert tempo == pytest.approx(90.0, abs=2.0)


def test_silence_has_no_tempo() -> None:
    assert (
        TrackMetadataReader().estimate_tempo(torch.zeros(2, 22050 * 5), 22050) is None
    )


def test_measures_duration_of_the_longest_stem() -> None:
    reader = TrackMetadataReader()

    duration = reader.measure_duration(
        {"vocals": torch.zeros(2, 44100), "drums": torch.zeros(2, 88200)},
        sample_rate=44100,
    )

    assert duration == pytest.approx(2.0)


def test_reads_everything_at_once(
    click_track: Callable[..., tuple[torch.Tensor, int]],
    tagged_mp3: Callable[[str, str], bytes],
) -> None:
    drums, sample_rate = click_track(bpm=120.0)

    metadata = TrackMetadataReader().read(
        data=tagged_mp3("Runaway", "Kanye West"),
        stems={"drums": drums, "vocals": torch.zeros_like(drums)},
        sample_rate=sample_rate,
    )

    assert metadata.title == "Runaway"
    assert metadata.artist == "Kanye West"
    assert metadata.duration_seconds == pytest.approx(12.0, abs=0.1)
    assert metadata.tempo_bpm == pytest.approx(120.0, abs=2.0)


def test_percussive_stem_name_is_configurable(
    click_track: Callable[..., tuple[torch.Tensor, int]],
) -> None:
    percussion, sample_rate = click_track(bpm=120.0)
    reader = TrackMetadataReader(percussive_stem="percussion")

    metadata = reader.read(
        data=b"not audio",
        stems={"percussion": percussion},
        sample_rate=sample_rate,
    )

    assert metadata.tempo_bpm == pytest.approx(120.0, abs=2.0)


def test_tempo_is_unknown_without_the_percussive_stem(
    click_track: Callable[..., tuple[torch.Tensor, int]],
) -> None:
    drums, sample_rate = click_track(bpm=120.0)

    metadata = TrackMetadataReader().read(
        data=b"not audio",
        stems={"vocals": drums},
        sample_rate=sample_rate,
    )

    assert metadata.tempo_bpm is None


def test_beat_based_tempo_needs_enough_beats(
    click_track: Callable[..., tuple[torch.Tensor, int]],
) -> None:
    tensor, sample_rate = click_track(bpm=120.0)
    reader = TrackMetadataReader(minimum_beats=1000)

    tempo = reader.estimate_tempo(tensor, sample_rate)

    assert tempo == pytest.approx(117.45, abs=0.1)
