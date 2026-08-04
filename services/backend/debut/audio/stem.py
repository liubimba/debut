from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Stem:
    path: Path
    name: str
