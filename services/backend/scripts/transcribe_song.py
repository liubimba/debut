import argparse
from pathlib import Path

from debut.export import notes_to_midi, notes_to_wav
from debut.transcription.song_transcriber import SongTranscriber


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("song", type=Path)
    parser.add_argument("out", type=Path)
    parser.add_argument("--wav", type=Path)
    args = parser.parse_args()

    with args.song.open("rb") as handle:
        notes = SongTranscriber().transcribe(handle)
    notes_to_midi(notes, args.out)
    if args.wav is not None:
        notes_to_wav(notes, args.wav)

    print(f"{len(notes)} notes -> {args.out}")


if __name__ == "__main__":
    main()
