import { describe, expect, it } from "vitest";
import { encodeWav } from "./wav";

const readBack = async (blob: Blob) => {
	const view = new DataView(await blob.arrayBuffer());
	const ascii = (offset: number, length: number) =>
		String.fromCharCode(
			...Array.from({ length }, (_, i) => view.getUint8(offset + i)),
		);
	return {
		riff: ascii(0, 4),
		wave: ascii(8, 4),
		channels: view.getUint16(22, true),
		sampleRate: view.getUint32(24, true),
		bitsPerSample: view.getUint16(34, true),
		dataBytes: view.getUint32(40, true),
		sampleAt: (index: number) => view.getInt16(44 + index * 2, true),
	};
};

describe("encodeWav", () => {
	it("writes a header a browser will accept", async () => {
		const header = await readBack(encodeWav(new Float32Array(8), 44100));
		expect(header.riff).toBe("RIFF");
		expect(header.wave).toBe("WAVE");
		expect(header.channels).toBe(1);
		expect(header.sampleRate).toBe(44100);
		expect(header.bitsPerSample).toBe(16);
		expect(header.dataBytes).toBe(16);
	});

	it("keeps the declared sample rate rather than assuming 44100", async () => {
		const header = await readBack(encodeWav(new Float32Array(4), 48000));
		expect(header.sampleRate).toBe(48000);
		expect(header.dataBytes).toBe(8);
	});

	it("normalises instead of clipping when the mix sums above unity", async () => {
		const header = await readBack(
			encodeWav(new Float32Array([1.6, -1.6]), 44100),
		);
		expect(header.sampleAt(0)).toBeLessThan(32767);
		expect(header.sampleAt(0)).toBeGreaterThan(32000);
		expect(header.sampleAt(1)).toBe(-header.sampleAt(0));
	});

	it("leaves a quiet mix at its original level", async () => {
		const header = await readBack(encodeWav(new Float32Array([0.5]), 44100));
		expect(header.sampleAt(0)).toBe(Math.round(0.5 * 32767));
	});

	it("produces an empty but valid file for an empty buffer", async () => {
		const header = await readBack(encodeWav(new Float32Array(0), 44100));
		expect(header.dataBytes).toBe(0);
		expect(header.riff).toBe("RIFF");
	});
});
