import type { Note } from "../audio/types";

export type Viewport = {
	widthCss: number;
	heightCss: number;
	pxPerSec: number;
	playheadX: number;
	midiLo: number;
	midiHi: number;
};

export const MIN_MIDI_SPAN = 18;
export const MIDI_PADDING = 3;

export const midiRangeOf = (notes: Note[]) => {
	if (notes.length === 0) return { midiLo: 55, midiHi: 55 + MIN_MIDI_SPAN };

	let lowest = Number.POSITIVE_INFINITY;
	let highest = Number.NEGATIVE_INFINITY;
	for (const note of notes) {
		if (note.pitch.midi < lowest) lowest = note.pitch.midi;
		if (note.pitch.midi > highest) highest = note.pitch.midi;
	}

	let midiLo = lowest - MIDI_PADDING;
	let midiHi = highest + MIDI_PADDING;
	const shortfall = MIN_MIDI_SPAN - (midiHi - midiLo);
	if (shortfall > 0) {
		midiLo -= Math.floor(shortfall / 2);
		midiHi += Math.ceil(shortfall / 2);
	}
	return { midiLo, midiHi };
};

export const createViewport = (
	widthCss: number,
	heightCss: number,
	notes: Note[],
	visibleSeconds: number,
	playheadFraction: number,
): Viewport => ({
	widthCss,
	heightCss,
	pxPerSec: visibleSeconds > 0 ? widthCss / visibleSeconds : 0,
	playheadX: widthCss * playheadFraction,
	...midiRangeOf(notes),
});

export const rowHeight = (view: Viewport) =>
	view.heightCss / Math.max(1, view.midiHi - view.midiLo);

export const timeToX = (view: Viewport, seconds: number, nowSeconds: number) =>
	view.playheadX + (seconds - nowSeconds) * view.pxPerSec;

export const xToTime = (view: Viewport, x: number, nowSeconds: number) =>
	view.pxPerSec === 0
		? nowSeconds
		: nowSeconds + (x - view.playheadX) / view.pxPerSec;

export const midiToY = (view: Viewport, midi: number) =>
	view.heightCss - (midi - view.midiLo + 0.5) * rowHeight(view);

export const visibleWindow = (view: Viewport, nowSeconds: number) => {
	if (view.pxPerSec === 0) return { from: nowSeconds, to: nowSeconds };
	return {
		from: nowSeconds - view.playheadX / view.pxPerSec,
		to: nowSeconds + (view.widthCss - view.playheadX) / view.pxPerSec,
	};
};

export const noteIsVisible = (note: Note, from: number, to: number) =>
	note.end_time >= from && note.start_time <= to;
