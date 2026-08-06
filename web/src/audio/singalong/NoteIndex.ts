import type { Note } from "../types";

export class NoteIndex {
	private readonly notes: Note[];
	private cursor = 0;

	constructor(notes: Note[]) {
		this.notes = [...notes].sort((a, b) => a.start_time - b.start_time);
	}

	get length() {
		return this.notes.length;
	}

	get endTime() {
		return this.notes.at(-1)?.end_time ?? 0;
	}

	at(seconds: number): Note | null {
		if (this.notes.length === 0) return null;

		if (
			this.cursor >= this.notes.length ||
			(this.notes[this.cursor]?.start_time ?? 0) > seconds
		) {
			this.cursor = 0;
		}

		while (
			this.cursor + 1 < this.notes.length &&
			(this.notes[this.cursor + 1]?.start_time ?? 0) <= seconds
		) {
			this.cursor += 1;
		}

		const candidate = this.notes[this.cursor];
		if (!candidate) return null;
		return candidate.start_time <= seconds && seconds <= candidate.end_time
			? candidate
			: null;
	}

	nextAfter(seconds: number): Note | null {
		for (const note of this.notes) {
			if (note.start_time > seconds) return note;
		}
		return null;
	}

	reset() {
		this.cursor = 0;
	}
}
