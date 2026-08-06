import { describe, expect, it } from "vitest";
import { contrastRatio, parseColor } from "./contrast";

describe("parseColor", () => {
	it("reads the rgb() form browsers return from getComputedStyle", () => {
		expect(parseColor("rgb(27, 23, 18)")).toEqual([27, 23, 18]);
	});

	it("reads oklch() so tokens authored in oklch are not silently skipped", () => {
		const [r, g, b] = parseColor("oklch(0.2075 0.0115 73.3)");
		expect(r).toBeCloseTo(27, 0);
		expect(g).toBeCloseTo(23, 0);
		expect(b).toBeCloseTo(18, 0);
	});

	it("throws instead of returning a wrong colour when the format is unknown", () => {
		expect(() => parseColor("transparent")).toThrow();
	});
});

describe("contrastRatio", () => {
	it("gives 21 for black on white", () => {
		expect(contrastRatio([0, 0, 0], [255, 255, 255])).toBeCloseTo(21, 5);
	});

	it("gives 1 for a colour against itself", () => {
		expect(contrastRatio([229, 97, 43], [229, 97, 43])).toBeCloseTo(1, 5);
	});

	it("is symmetric, so pair order in contrast-pairs.ts cannot flip a verdict", () => {
		const fg: [number, number, number] = [237, 230, 216];
		const bg: [number, number, number] = [27, 23, 18];
		expect(contrastRatio(fg, bg)).toBeCloseTo(contrastRatio(bg, fg), 10);
	});

	it("scores cream on warm near-black above the 4.5 body-text threshold", () => {
		expect(contrastRatio([237, 230, 216], [27, 23, 18])).toBeGreaterThan(4.5);
	});
});
