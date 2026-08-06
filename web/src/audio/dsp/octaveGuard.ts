import { centsBetween } from "./notes";

export type OctaveGuardOptions = {
	toleranceCents: number;
	maxOctaves: number;
};

export const DEFAULT_GUARD: OctaveGuardOptions = {
	toleranceCents: 60,
	maxOctaves: 2,
};

export const correctOctave = (
	freqHz: number,
	referenceHz: number | null,
	options: OctaveGuardOptions = DEFAULT_GUARD,
) => {
	if (freqHz <= 0 || referenceHz === null || referenceHz <= 0) return freqHz;

	let bestFreq = freqHz;
	let bestDistance = Math.abs(centsBetween(freqHz, referenceHz));

	for (
		let octave = -options.maxOctaves;
		octave <= options.maxOctaves;
		octave += 1
	) {
		if (octave === 0) continue;
		const shifted = freqHz * 2 ** octave;
		const distance = Math.abs(centsBetween(shifted, referenceHz));
		if (distance < bestDistance) {
			bestDistance = distance;
			bestFreq = shifted;
		}
	}

	return bestDistance <= options.toleranceCents ? bestFreq : freqHz;
};

export const pickReference = (
	previousHz: number | null,
	targetHz: number | null,
) => targetHz ?? previousHz;
