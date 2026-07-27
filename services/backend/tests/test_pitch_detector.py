import pytest
from separator.pitch_detector import PitchDetector


def test_pitch_detector_detects_a4_pure_sine(sine_stereo):
    wav, sr = sine_stereo(freq_hz=440.0)

    f0, periodicity = PitchDetector().detect_pitch(wav, sr=sr)
    voiced = periodicity > 0.5
    median_hz = f0[voiced].median().item()
    assert median_hz == pytest.approx(440.0, abs=5.0)
