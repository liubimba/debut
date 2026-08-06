import type { Note } from "../../audio/types";
import { buildTrack, type Step, trackDuration } from "./buildTrack";

export type SongFixture = {
	id: string;
	title: string;
	artist: string;
	bpm: number;
	keyName: string;
	notes: Note[];
	durationSec: number;
	chords: number[][];
};

const BPM = 92;

const auroraLinesMelody: Step[] = [
	{ midi: 69, beats: 1 },
	{ midi: 71, beats: 1 },
	{ midi: 72, beats: 2 },
	{ midi: 71, beats: 1 },
	{ midi: 69, beats: 1 },
	{ midi: 67, beats: 2 },
	{ midi: 0, beats: 1, rest: true },
	{ midi: 67, beats: 1 },
	{ midi: 69, beats: 1 },
	{ midi: 72, beats: 3 },
	{ midi: 0, beats: 1, rest: true },
	{ midi: 74, beats: 1 },
	{ midi: 72, beats: 1 },
	{ midi: 71, beats: 2 },
	{ midi: 69, beats: 2 },
	{ midi: 67, beats: 1 },
	{ midi: 65, beats: 1 },
	{ midi: 64, beats: 4 },
	{ midi: 0, beats: 2, rest: true },
	{ midi: 64, beats: 1 },
	{ midi: 67, beats: 1 },
	{ midi: 69, beats: 2 },
	{ midi: 71, beats: 2 },
	{ midi: 72, beats: 4 },
	{ midi: 0, beats: 2, rest: true },
];

const G_MAJOR_DEGREES = [0, 2, 4, 5, 7, 9, 11];
const G3 = 55;

const ascending = [
	...Array.from({ length: 14 }, (_, index) => {
		const degree = G_MAJOR_DEGREES[index % 7] ?? 0;
		return G3 + degree + 12 * Math.floor(index / 7);
	}),
	G3 + 24,
];

const scaleSteps: Step[] = [
	...ascending,
	...[...ascending].reverse().slice(1),
].map((midi) => ({
	midi,
	beats: 1,
}));

const notes = buildTrack(auroraLinesMelody, BPM, 0.5);
const scaleNotes = buildTrack(scaleSteps, 120, 0.5);

export const AURORA_LINES: SongFixture = {
	id: "aurora-lines",
	title: "Aurora Lines",
	artist: "Debut demo",
	bpm: BPM,
	keyName: "C major",
	notes,
	durationSec: trackDuration(notes) + 1,
	chords: [
		[45, 52, 57, 60],
		[41, 48, 53, 57],
		[43, 50, 55, 59],
		[48, 55, 60, 64],
	],
};

export const TWO_OCTAVE_SCALE: SongFixture = {
	id: "two-octave-scale",
	title: "Two-octave scale",
	artist: "Debut demo",
	bpm: 120,
	keyName: "G major",
	notes: scaleNotes,
	durationSec: trackDuration(scaleNotes) + 1,
	chords: [[43, 50, 55, 59]],
};

const droneNotes = buildTrack([{ midi: 69, beats: 30 }], 60, 0.5);

export const SUSTAINED_A: SongFixture = {
	id: "sustained-a",
	title: "Sustained A — warm-up",
	artist: "Debut demo",
	bpm: 60,
	keyName: "A",
	notes: droneNotes,
	durationSec: trackDuration(droneNotes) + 1,
	chords: [[45, 52, 57, 61]],
};

export const SONGS: SongFixture[] = [
	AURORA_LINES,
	TWO_OCTAVE_SCALE,
	SUSTAINED_A,
];

export const songById = (id: string) =>
	SONGS.find((song) => song.id === id) ?? AURORA_LINES;
