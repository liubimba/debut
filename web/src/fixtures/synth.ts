import { midiToHz } from "../audio/dsp/notes";
import type { SongFixture } from "./songs";

const SAMPLE_RATE = 44100;

const adsr = (
	buffer: Float32Array,
	start: number,
	length: number,
	attack: number,
	release: number,
) => {
	for (let i = 0; i < length; i += 1) {
		const index = start + i;
		if (index >= buffer.length) break;
		const fadeIn = Math.min(1, i / attack);
		const fadeOut = Math.min(1, (length - i) / release);
		buffer[index] = (buffer[index] ?? 0) * fadeIn * fadeOut;
	}
};

const addTone = (
	buffer: Float32Array,
	freqHz: number,
	startSec: number,
	durationSec: number,
	amplitude: number,
	harmonics: number[],
	vibratoCents = 0,
) => {
	const start = Math.floor(startSec * SAMPLE_RATE);
	const length = Math.floor(durationSec * SAMPLE_RATE);
	const weight = harmonics.reduce((sum, value) => sum + value, 0);
	const voice = new Float32Array(length);

	for (let i = 0; i < length; i += 1) {
		const t = i / SAMPLE_RATE;
		const detune =
			vibratoCents === 0
				? 1
				: 2 ** ((vibratoCents * Math.sin(2 * Math.PI * 5.2 * t)) / 1200);
		const f = freqHz * detune;
		let sample = 0;
		for (let h = 0; h < harmonics.length; h += 1) {
			sample += (harmonics[h] ?? 0) * Math.sin(2 * Math.PI * f * (h + 1) * t);
		}
		voice[i] = (amplitude * sample) / weight;
	}

	adsr(voice, 0, length, SAMPLE_RATE * 0.012, SAMPLE_RATE * 0.06);

	for (let i = 0; i < length; i += 1) {
		const index = start + i;
		if (index >= buffer.length) break;
		buffer[index] = (buffer[index] ?? 0) + (voice[i] ?? 0);
	}
};

export const renderInstrumental = (song: SongFixture) => {
	const buffer = new Float32Array(Math.ceil(song.durationSec * SAMPLE_RATE));
	const barSeconds = (60 / song.bpm) * 4;
	const bars = Math.ceil(song.durationSec / barSeconds);

	for (let bar = 0; bar < bars; bar += 1) {
		const chord = song.chords[bar % song.chords.length] ?? [];
		const at = bar * barSeconds;
		const root = chord[0];
		if (root !== undefined) {
			addTone(
				buffer,
				midiToHz(root - 12),
				at,
				barSeconds * 0.95,
				0.22,
				[1, 0.35, 0.12],
			);
		}
		for (const midi of chord.slice(1)) {
			addTone(
				buffer,
				midiToHz(midi),
				at,
				barSeconds * 0.9,
				0.07,
				[1, 0.5, 0.25, 0.12],
			);
		}
		for (let beat = 0; beat < 4; beat += 1) {
			addTone(
				buffer,
				1400,
				at + beat * (barSeconds / 4),
				0.03,
				beat === 0 ? 0.05 : 0.025,
				[1],
			);
		}
	}

	return buffer;
};

export const renderReferenceVocal = (song: SongFixture) => {
	const buffer = new Float32Array(Math.ceil(song.durationSec * SAMPLE_RATE));
	for (const note of song.notes) {
		addTone(
			buffer,
			note.pitch.freq_hz,
			note.start_time,
			note.end_time - note.start_time,
			0.3,
			[1, 0.45, 0.22, 0.1],
			25,
		);
	}
	return buffer;
};

export const SYNTH_SAMPLE_RATE = SAMPLE_RATE;
