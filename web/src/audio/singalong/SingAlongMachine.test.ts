import { describe, expect, it } from "vitest";
import { DEFAULT_CONFIG } from "../config";
import { Driver, note, offBy, silence } from "../testing/FakeClock";

const song = [note(69, 0, 4), note(71, 4, 8)];

describe("SingAlongMachine", () => {
	it("keeps the track running while the singer holds the note", () => {
		const driver = new Driver(song).advance(1000, offBy(69, 5));
		expect(driver.phase).toBe("singing");
		expect(driver.effects).toEqual([]);
	});

	it("pauses the track once the singer is sustainedly off the note", () => {
		const driver = new Driver(song).advance(1000, offBy(69, 300));
		expect(driver.phase).toBe("paused_out_of_note");
		expect(driver.effects).toEqual(["pause"]);
	});

	it("does NOT pause on a brief blip — this is the whole point of the hysteresis", () => {
		const driver = new Driver(song)
			.advance(500, offBy(69, 0))
			.advance(DEFAULT_CONFIG.exitHoldMs - 40, offBy(69, 300))
			.advance(500, offBy(69, 0));
		expect(driver.phase).toBe("singing");
		expect(driver.effects).toEqual([]);
	});

	it("resumes only after the singer has held the note back for enterHoldMs", () => {
		const driver = new Driver(song).advance(1000, offBy(69, 300));
		expect(driver.phase).toBe("paused_out_of_note");

		driver.advance(DEFAULT_CONFIG.enterHoldMs - 20, offBy(69, 0));
		expect(driver.phase).toBe("paused_out_of_note");

		driver.advance(60, offBy(69, 0));
		expect(driver.phase).toBe("singing");
		expect(driver.effects).toEqual(["pause", "play"]);
	});

	it("uses a wider gate to pause than to resume, so the edge does not chatter", () => {
		const between = (DEFAULT_CONFIG.enterCents + DEFAULT_CONFIG.exitCents) / 2;
		const singing = new Driver(song).advance(1000, offBy(69, between));
		expect(singing.phase).toBe("singing");

		const paused = new Driver(song)
			.advance(1000, offBy(69, 300))
			.advance(1000, offBy(69, between));
		expect(paused.phase).toBe("paused_out_of_note");
	});

	it("tolerates a short breath before deciding the singer stopped", () => {
		const driver = new Driver(song)
			.advance(500, offBy(69, 0))
			.advance(DEFAULT_CONFIG.unvoicedGraceMs, silence)
			.advance(300, offBy(69, 0));
		expect(driver.phase).toBe("singing");
		expect(driver.effects).toEqual([]);
	});

	it("pauses when the singer simply stops for good", () => {
		const driver = new Driver(song)
			.advance(500, offBy(69, 0))
			.advance(
				DEFAULT_CONFIG.exitHoldMs + DEFAULT_CONFIG.unvoicedGraceMs + 100,
				silence,
			);
		expect(driver.phase).toBe("paused_out_of_note");
		expect(driver.effects).toEqual(["pause"]);
	});

	it("corrects an octave slip instead of pausing the singer who is actually right", () => {
		const driver = new Driver(song).advance(1000, offBy(57, 0));
		expect(driver.phase).toBe("singing");
		expect(Math.abs(driver.cents)).toBeLessThan(5);
	});

	it("does not judge pitch during a rest", () => {
		const withRest = [note(69, 0, 1), note(71, 3, 5)];
		const driver = new Driver(withRest).seek(1.5).advance(1000, offBy(48, 400));
		expect(driver.phase).toBe("singing");
		expect(driver.hit).toBe("rest");
		expect(driver.effects).toEqual([]);
	});

	it("lets a paused singer through a rest rather than stranding them", () => {
		const withRest = [note(69, 0, 1), note(71, 3, 5)];
		const driver = new Driver(withRest).advance(600, offBy(69, 300));
		expect(driver.phase).toBe("paused_out_of_note");

		driver.seek(1.5).advance(100, silence);
		expect(driver.phase).toBe("singing");
		expect(driver.effects).toEqual(["pause", "play"]);
	});

	it("reports direction so the indicator can say high or low, not just wrong", () => {
		expect(new Driver(song).advance(200, offBy(69, 70)).hit).toBe("sharp");
		expect(new Driver(song).advance(200, offBy(69, -70)).hit).toBe("flat");
	});

	it("finishes when the track runs past its last note", () => {
		const driver = new Driver(song).seek(9).advance(50, offBy(71, 0));
		expect(driver.phase).toBe("done");
		expect(driver.effects).toEqual(["finish"]);
	});

	it("grows the streak while singing and resets it on a pause", () => {
		const driver = new Driver(song).advance(1000, offBy(69, 0));
		expect(driver.streakSec).toBeGreaterThan(0.9);

		driver.advance(1000, offBy(69, 300));
		expect(driver.streakSec).toBe(0);
	});

	it("honours a raised tolerance instead of hardcoding 50 cents", () => {
		const lenient = { ...DEFAULT_CONFIG, enterCents: 100, exitCents: 150 };
		const driver = new Driver(song, lenient).advance(1000, offBy(69, 120));
		expect(driver.phase).toBe("singing");
	});

	it("shifts the note lookup by the latency compensation", () => {
		const shifted = { ...DEFAULT_CONFIG, latencyCompensationMs: 4000 };
		const driver = new Driver(song, shifted)
			.seek(4.5)
			.advance(200, offBy(69, 0));
		expect(driver.target?.pitch.midi).toBe(69);
	});
});
