import { describe, expect, it } from "vitest";
import { makeNoise, makeSilence, makeTone } from "../testing/synthetic";
import { centsBetween } from "./notes";
import { createYinDetector } from "./yin";

const SAMPLE_RATE = 48000;
const WINDOW = 2048;

const detector = createYinDetector({
	sampleRate: SAMPLE_RATE,
	windowSize: WINDOW,
	fminHz: 65,
	fmaxHz: 1200,
	threshold: 0.15,
});

describe("yin", () => {
	it.each([98, 146.83, 220, 440, 659.25, 880])(
		"tracks a sine at %d Hz within 5 cents",
		(freqHz) => {
			const { freqHz: detected } = detector(
				makeTone({ freqHz, sampleRate: SAMPLE_RATE, length: WINDOW }),
			);
			expect(Math.abs(centsBetween(detected, freqHz))).toBeLessThan(5);
		},
	);

	it("tracks a harmonic-rich tone without dropping an octave", () => {
		const { freqHz } = detector(
			makeTone({
				freqHz: 196,
				sampleRate: SAMPLE_RATE,
				length: WINDOW,
				harmonics: [1, 0.8, 0.6, 0.4, 0.25],
			}),
		);
		expect(Math.abs(centsBetween(freqHz, 196))).toBeLessThan(10);
	});

	it("survives a breathy voice — harmonics plus noise", () => {
		const { freqHz, confidence } = detector(
			makeTone({
				freqHz: 261.63,
				sampleRate: SAMPLE_RATE,
				length: WINDOW,
				harmonics: [1, 0.5, 0.3],
				noise: 0.05,
			}),
		);
		expect(Math.abs(centsBetween(freqHz, 261.63))).toBeLessThan(15);
		expect(confidence).toBeGreaterThan(0.5);
	});

	it("follows vibrato instead of reporting the centre frequency only", () => {
		const withVibrato = detector(
			makeTone({
				freqHz: 440,
				sampleRate: SAMPLE_RATE,
				length: WINDOW,
				vibratoCents: 60,
				vibratoHz: 6,
			}),
		);
		expect(withVibrato.confidence).toBeGreaterThan(0.5);
		expect(Math.abs(centsBetween(withVibrato.freqHz, 440))).toBeLessThan(70);
	});

	it("reports silence for an empty buffer rather than a phantom pitch", () => {
		expect(detector(makeSilence(WINDOW))).toEqual({ freqHz: 0, confidence: 0 });
	});

	it("gives white noise low confidence so the gate rejects it", () => {
		const { confidence } = detector(makeNoise(WINDOW));
		expect(confidence).toBeLessThan(0.5);
	});

	it("returns silence when the buffer is shorter than the window", () => {
		expect(
			detector(makeTone({ freqHz: 440, sampleRate: SAMPLE_RATE, length: 512 })),
		).toEqual({
			freqHz: 0,
			confidence: 0,
		});
	});

	it("allocates nothing per call, so it is safe on the audio thread", () => {
		const buffer = makeTone({
			freqHz: 440,
			sampleRate: SAMPLE_RATE,
			length: WINDOW,
		});
		const first = detector(buffer);
		for (let i = 0; i < 200; i += 1) detector(buffer);
		expect(detector(buffer)).toEqual(first);
	});
});
