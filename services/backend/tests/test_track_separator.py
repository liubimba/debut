import pytest

from separator.track_separator import TrackSeparator


def test_track_separator_vocals_stem(sine_stereo):
    wav, sr = sine_stereo(freq_hz=440.0)

    stems = TrackSeparator().separate_audio(wav, sr)
    assert "vocals" in stems
    assert stems["vocals"].dim() == 2


@pytest.mark.slow
def test_track_separator_separates_file_with_vocal_track(sine_wav_file_path):
    stems = TrackSeparator().separate_file(audio_file=sine_wav_file_path())
    assert "vocals" in stems
    assert stems["vocals"].dim() == 2


@pytest.mark.slow
def test_track_separator_separates_real_music_file(data_dir):
    stems = TrackSeparator().separate_file(audio_file=data_dir / "runaway.mp3")
    assert "vocals" in stems
    assert stems["vocals"].dim() == 2
