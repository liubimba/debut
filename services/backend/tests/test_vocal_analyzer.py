import math
import random

import separator


def test_streaming_pitch_analyzer_chunk(sine_stereo) -> None:
    threshold: float = 15
    fmin: float = 65.0
    fmax: float = 1400
    freq_hz: float = random.random() * (fmax - fmin) + fmin
    for i in range(20):
        wav, sr = sine_stereo(freq_hz=freq_hz)
        pitch_detector = separator.PitchDetector(fmax=fmax, fmin=fmin, model="full")
        f0, periodicity = separator.StreamingPitchAnalyzer(pitch_detector=pitch_detector).push_chunk(wav)
        assert math.fabs(f0 - freq_hz) < threshold


def test_offline_pitch_analyzer_audio(sine_wav_file_path) -> None:
    threshold: float = 15
    fmax: float = 1400
    fmin: float = 65.0
    sample_rate: int = 44100
    freq_hz: float = random.random() * (fmax - fmin) + fmin
    offline_analyzer = separator.OfflinePitchAnalyzer(
        pitch_detector=separator.PitchDetector(fmax=fmax, fmin=fmin, model="full"),
        track_separator=separator.TrackSeparator(sample_rate=sample_rate))
    f0, periodicity = offline_analyzer.analyze_file(audio_file=sine_wav_file_path(freq_hz=freq_hz, sr=sample_rate))
    assert all(map(lambda freq: math.fabs(freq - freq_hz) - threshold, f0[0]))
