import threading
import uuid
from os import walk

import torchaudio
from torch import Tensor

from debut.audio.stem import Stem
from debut.path.app_path import AppPath


class StemsStore:
    def __init__(self, app_path: AppPath) -> None:
        self._app_path = app_path
        self._lock = threading.RLock()

    def list_stems(self, stem_id: str) -> list[Stem]:
        with self._lock:
            path = (self._app_path.stems() / stem_id).resolve()
            if not path.is_relative_to(self._app_path.stems().resolve()):
                raise ValueError(f"Stem {stem_id} is not relative to stems path")
            if not path.exists():
                return []
            filenames = next(walk(path), (None, None, []))[2]
            return [Stem(name=name, path=path / name) for name in filenames]

    def list_all_stems(self) -> dict[str, list[Stem]]:
        with self._lock:
            dirnames = next(walk(self._app_path.stems()), (None, [], None))[1]
            return {dirname: self.list_stems(dirname) for dirname in dirnames}

    def save(
        self, stems: dict[str, Tensor], sample_rate: int, stem_id: str | None = None
    ) -> list[Stem]:
        with self._lock:
            if stem_id is None:
                stem_id = uuid.uuid4().hex
            stem_path = self._app_path.stems() / stem_id
            stem_path.mkdir(parents=True, exist_ok=True)
            for stem_name, stem_tensor in stems.items():
                torchaudio.save(
                    uri=stem_path / f"{stem_name}.wav",
                    src=stem_tensor,
                    sample_rate=sample_rate,
                    format="wav",
                )
            return self.list_stems(stem_id)
