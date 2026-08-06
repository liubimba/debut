import { midiToHz } from "../../audio/dsp/notes";
import type { Note } from "../../audio/types";

export type Step = {
	midi: number;
	beats: number;
	rest?: boolean;
};

export const buildTrack = (steps: Step[], bpm: number, startAt = 0): Note[] => {
	const secondsPerBeat = 60 / bpm;
	const notes: Note[] = [];
	let cursor = startAt;

	for (const step of steps) {
		const duration = step.beats * secondsPerBeat;
		if (!step.rest) {
			notes.push({
				pitch: {
					midi: step.midi,
					freq_hz: midiToHz(step.midi),
					cents_offset: 0,
					confidence: 1,
				},
				start_time: cursor,
				end_time: cursor + duration * 0.92,
			});
		}
		cursor += duration;
	}

	return notes;
};

export const trackDuration = (notes: Note[]) =>
	notes.reduce((longest, note) => Math.max(longest, note.end_time), 0);
