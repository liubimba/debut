import torch
import torchcrepe


class PitchDetector:
    def __init__(self, fmin: float = 65.0, fmax: float = 1200.0, model: str = "tiny",
                 device: str | None = None):
        self._fmin = fmin
        self._fmax = fmax
        self._model = model
        self._device = device or ("cuda" if torch.cuda.is_available() else "cpu")

    def detect_pitch(self, tensor: torch.Tensor, sr: int) -> tuple[torch.Tensor, torch.Tensor]:
        audio: torch.Tensor = self._to_mono(tensor)
        return torchcrepe.predict(
            audio,
            sample_rate=sr,
            return_periodicity=True,
            fmin=self._fmin,
            fmax=self._fmax,
            device=self._device,
            model=self._model
        )

    @staticmethod
    def _to_mono(tensor: torch.Tensor) -> torch.Tensor:
        if tensor.dim() == 1:
            return tensor.unsqueeze(0)
        return tensor.mean(dim=0, keepdim=True)
