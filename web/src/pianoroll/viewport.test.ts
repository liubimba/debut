import { describe, expect, it } from "vitest";
import type { Note } from "../audio/types";
import {
	createViewport,
	MIN_MIDI_SPAN,
	midiRangeOf,
	midiToY,
	noteIsVisible,
	rowHeight,
	timeToX,
	visibleWindow,
	xToTime,
} from "./viewport";

const note = (midi: number, start: number, end: number): Note => ({
	pitch: { midi, freq_hz: 440, cents_offset: 0, confidence: 0.9 },
	start_time: start,
	end_time: end,
});

const view = (notes: Note[] = [note(60, 0, 1), note(72, 1, 2)]) =>
	createViewport(390, 300, notes, 4, 0.25);

describe("midiRangeOf", () => {
	it("pads the song range so notes never touch the edge", () => {
		expect(midiRangeOf([note(55, 0, 1), note(74, 1, 2)])).toEqual({
			midiLo: 52,
			midiHi: 77,
		});
	});

	it("applies the minimum span on top of the padding, not instead of it", () => {
		const { midiLo, midiHi } = midiRangeOf([note(60, 0, 1), note(64, 1, 2)]);
		expect(midiHi - midiLo).toBe(MIN_MIDI_SPAN);
		expect(midiLo).toBeLessThanOrEqual(57);
		expect(midiHi).toBeGreaterThanOrEqual(67);
	});

	it("widens a narrow song to the minimum span so rows are not absurdly tall", () => {
		const { midiLo, midiHi } = midiRangeOf([note(60, 0, 1)]);
		expect(midiHi - midiLo).toBe(MIN_MIDI_SPAN);
		expect(midiLo).toBeLessThan(60);
		expect(midiHi).toBeGreaterThan(60);
	});

	it("does not shrink a song that is already wider than the minimum", () => {
		const { midiLo, midiHi } = midiRangeOf([note(40, 0, 1), note(90, 1, 2)]);
		expect(midiLo).toBe(37);
		expect(midiHi).toBe(93);
	});

	it("returns a usable range for an empty track instead of infinities", () => {
		const { midiLo, midiHi } = midiRangeOf([]);
		expect(Number.isFinite(midiLo)).toBe(true);
		expect(midiHi - midiLo).toBe(MIN_MIDI_SPAN);
	});
});

describe("time mapping", () => {
	it("puts the current moment exactly on the playhead", () => {
		const v = view();
		expect(timeToX(v, 10, 10)).toBeCloseTo(v.playheadX, 10);
	});

	it("scrolls the future to the right of the playhead", () => {
		const v = view();
		expect(timeToX(v, 11, 10)).toBeGreaterThan(v.playheadX);
	});

	it("round-trips through xToTime", () => {
		const v = view();
		expect(xToTime(v, timeToX(v, 12.5, 10), 10)).toBeCloseTo(12.5, 10);
	});

	it("shows the configured number of seconds across the full width", () => {
		const v = view();
		const { from, to } = visibleWindow(v, 10);
		expect(to - from).toBeCloseTo(4, 10);
	});

	it("keeps a quarter of the window behind the playhead so you see what you just sang", () => {
		const v = view();
		const { from } = visibleWindow(v, 10);
		expect(10 - from).toBeCloseTo(1, 10);
	});

	it("survives a zero-width canvas instead of dividing by zero", () => {
		const v = createViewport(0, 300, [note(60, 0, 1)], 4, 0.25);
		expect(Number.isFinite(timeToX(v, 5, 0))).toBe(true);
		expect(xToTime(v, 100, 7)).toBe(7);
	});
});

describe("pitch mapping", () => {
	it("puts a lower note lower on the screen", () => {
		const v = view();
		expect(midiToY(v, 60)).toBeGreaterThan(midiToY(v, 72));
	});

	it("centres a note inside its row", () => {
		const v = view();
		expect(midiToY(v, v.midiLo)).toBeCloseTo(
			v.heightCss - rowHeight(v) / 2,
			10,
		);
	});

	it("maps continuous midi so cents become sub-row movement, not a step", () => {
		const v = view();
		const flat = midiToY(v, 60 - 0.25);
		expect(flat).toBeGreaterThan(midiToY(v, 60));
		expect(flat).toBeLessThan(midiToY(v, 59));
	});
});

describe("noteIsVisible", () => {
	it("keeps a note that straddles the left edge", () => {
		expect(noteIsVisible(note(60, 8, 12), 10, 14)).toBe(true);
	});

	it("drops a note that ended before the window", () => {
		expect(noteIsVisible(note(60, 1, 2), 10, 14)).toBe(false);
	});

	it("drops a note that starts after the window", () => {
		expect(noteIsVisible(note(60, 20, 21), 10, 14)).toBe(false);
	});
});
