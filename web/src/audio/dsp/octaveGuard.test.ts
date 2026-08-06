import { describe, expect, it } from "vitest";
import { centsBetween } from "./notes";
import { correctOctave } from "./octaveGuard";

describe("correctOctave", () => {
	it("lifts a half-frequency slip back onto the target note", () => {
		expect(correctOctave(110, 220)).toBeCloseTo(220, 5);
	});

	it("drops a doubled reading back onto the target note", () => {
		expect(correctOctave(880, 440)).toBeCloseTo(440, 5);
	});

	it("leaves an honest reading alone", () => {
		expect(correctOctave(440, 440)).toBe(440);
	});

	it("does not snap a genuine leap of a fifth into an octave error", () => {
		expect(correctOctave(330, 220)).toBe(330);
	});

	it("keeps a slightly flat octave slip, still flat, after correction", () => {
		const corrected = correctOctave(109.4, 220);
		expect(Math.abs(centsBetween(corrected, 220))).toBeLessThan(20);
		expect(corrected).toBeLessThan(220);
	});

	it("passes silence through untouched", () => {
		expect(correctOctave(0, 220)).toBe(0);
	});

	it("passes the reading through when there is no reference yet", () => {
		expect(correctOctave(110, null)).toBe(110);
	});

	it("corrects two octaves down, which happens on low male voices", () => {
		expect(correctOctave(110, 440)).toBeCloseTo(440, 5);
	});
});
