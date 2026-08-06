export type ToneOptions = {
	freqHz: number;
	sampleRate: number;
	length: number;
	amplitude?: number;
	harmonics?: number[];
	noise?: number;
	vibratoCents?: number;
	vibratoHz?: number;
	phase?: number;
};

const mulberry32 = (seed: number) => {
	let state = seed >>> 0;
	return () => {
		state = (state + 0x6d2b79f5) >>> 0;
		let t = state;
		t = Math.imul(t ^ (t >>> 15), t | 1);
		t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
};

export const makeTone = ({
	freqHz,
	sampleRate,
	length,
	amplitude = 0.4,
	harmonics = [1],
	noise = 0,
	vibratoCents = 0,
	vibratoHz = 5,
	phase = 0,
}: ToneOptions) => {
	const buffer = new Float32Array(length);
	const random = mulberry32(Math.round(freqHz * 1000) + length);
	const weight = harmonics.reduce((sum, value) => sum + value, 0);

	for (let i = 0; i < length; i += 1) {
		const t = i / sampleRate;
		const detune = vibratoCents * Math.sin(2 * Math.PI * vibratoHz * t);
		const f = freqHz * 2 ** (detune / 1200);
		let sample = 0;
		for (let h = 0; h < harmonics.length; h += 1) {
			sample +=
				(harmonics[h] ?? 0) * Math.sin(2 * Math.PI * f * (h + 1) * t + phase);
		}
		buffer[i] = (amplitude * sample) / weight + noise * (random() * 2 - 1);
	}

	return buffer;
};

export const makeSilence = (length: number) => new Float32Array(length);

export const makeNoise = (length: number, amplitude = 0.2) => {
	const buffer = new Float32Array(length);
	const random = mulberry32(length);
	for (let i = 0; i < length; i += 1)
		buffer[i] = amplitude * (random() * 2 - 1);
	return buffer;
};
