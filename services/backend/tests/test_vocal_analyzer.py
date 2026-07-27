import math
import random

import separator


def test_streaming_pitch_analyzer_chunk(sine_stereo) -> None:
    tolerance_cents: float = 50
    fmin: float = 65.0
    fmax: float = 1400
    freq_hz: float = random.random() * (fmax - fmin) + fmin
    for i in range(20):
        wav, sr = sine_stereo(freq_hz=freq_hz)
        pitch_detector = separator.PitchDetector(fmax=fmax, fmin=fmin, model="tiny")
        f0, periodicity = separator.StreamingPitchAnalyzer(
            pitch_detector=pitch_detector
        ).push_chunk(wav)
        cents = 1200 * math.log2(f0 / freq_hz)
        assert math.fabs(cents) < tolerance_cents, (
            f"{freq_hz=}, {f0=}, {periodicity=}, {cents=}"
        )


def test_offline_pitch_analyzer_audio(sine_wav_file_path) -> None:
    tolerance_cents: float = 50
    fmax: float = 1400
    fmin: float = 65.0
    sample_rate: int = 44100
    freq_hz: float = random.random() * (fmax - fmin) + fmin
    offline_analyzer = separator.OfflinePitchAnalyzer(
        pitch_detector=separator.PitchDetector(fmax=fmax, fmin=fmin, model="tiny"),
        track_separator=separator.TrackSeparator(sample_rate=sample_rate),
    )
    f0, periodicity = offline_analyzer.analyze_file(
        audio_file=sine_wav_file_path(freq_hz=freq_hz, sr=sample_rate)
    )
    assert all(
        map(
            lambda freq: math.fabs(1200 * math.log2(freq / freq_hz)) < tolerance_cents,
            f0[0],
        )
    )
