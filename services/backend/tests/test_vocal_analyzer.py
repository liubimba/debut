import math
import pathlib
import random
from unittest.mock import MagicMock

import separator


def test_streaming_pitch_analyzer_chunk(sine_stereo) -> None:
    random.seed(0)
    tolerance_cents: float = 50.0
    fmin, fmax = 65.0, 1400.0
    pitch_detector = separator.PitchDetector(fmax=fmax, fmin=fmin, model="tiny")
    checked = 0
    for _ in range(20):
        freq_hz = random.uniform(fmin, fmax)
        wav, _ = sine_stereo(freq_hz=freq_hz)
        f0, periodicity = separator.StreamingPitchAnalyzer(
            pitch_detector=pitch_detector
        ).push_chunk(wav)
        if periodicity < 0.5:
            continue
        checked += 1
        cents = 1200 * math.log2(f0 / freq_hz)
        assert math.fabs(cents) < tolerance_cents, (
            f"{freq_hz=}, {f0=}, {periodicity=}, {cents=}"
        )
    assert checked > 0


def test_offline_pitch_analyzer_recovers_pitch(sine_stereo) -> None:
    tolerance_cents: float = 50.0
    fmin, fmax = 65.0, 1400.0
    sample_rate = 44100
    freq_hz = random.random() * (fmax - fmin) + fmin
    wav, _ = sine_stereo(freq_hz=freq_hz, sr=sample_rate)

    track_separator = MagicMock()
    track_separator.separate_file.return_value = {"vocals": wav}
    track_separator.get_sample_rate.return_value = sample_rate

    offline_analyzer = separator.OfflinePitchAnalyzer(
        pitch_detector=separator.PitchDetector(fmax=fmax, fmin=fmin, model="tiny"),
        track_separator=track_separator,
    )
    f0, periodicity = offline_analyzer.analyze_file(
        audio_file=pathlib.Path("unused.wav")
    )

    voiced = periodicity[0] > 0.5
    median_hz = f0[0][voiced].median().item()
    cents = 1200 * math.log2(median_hz / freq_hz)
    assert math.fabs(cents) < tolerance_cents, f"{freq_hz=}, {median_hz=}, {cents=}"
