from pathlib import Path


class AppPath:
    def __init__(self, root: Path | None = None) -> None:
        self._root = root or (Path.home() / ".debut")
        self._root.mkdir(parents=True, exist_ok=True)

    def stems(self) -> Path:
        return self._root / "stems"
