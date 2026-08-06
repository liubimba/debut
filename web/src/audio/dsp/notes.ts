const NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];

export const A4_HZ = 440;
export const A4_MIDI = 69;

export const hzToMidiFloat = (hz: number) =>
	A4_MIDI + 12 * Math.log2(hz / A4_HZ);

export const midiToHz = (midi: number) => A4_HZ * 2 ** ((midi - A4_MIDI) / 12);

export const centsBetween = (hz: number, referenceHz: number) =>
	1200 * Math.log2(hz / referenceHz);

export const midiToName = (midi: number) => {
	const rounded = Math.round(midi);
	const name = NAMES[((rounded % 12) + 12) % 12];
	return `${name}${Math.floor(rounded / 12) - 1}`;
};

export const hzToPitch = (hz: number, confidence: number) => {
	const midiFloat = hzToMidiFloat(hz);
	const midi = Math.round(midiFloat);
	return {
		midi,
		freqHz: hz,
		centsOffset: (midiFloat - midi) * 100,
		confidence,
	};
};
