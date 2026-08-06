import type { EngineConfig } from "../config";
import { centsBetween } from "../dsp/notes";
import { correctOctave } from "../dsp/octaveGuard";
import type { Note, SingAlongPhase } from "../types";
import type { NoteIndex } from "./NoteIndex";

export type MachineInput = {
	nowMs: number;
	positionSec: number;
	freqHz: number;
	confidence: number;
};

export type Hit = "in-note" | "sharp" | "flat" | "silent" | "rest";

export type MachineState = {
	phase: SingAlongPhase;
	hit: Hit;
	cents: number;
	target: Note | null;
	streakSec: number;
	inNoteMs: number;
	outNoteMs: number;
	outCause: "silence" | "pitch";
	lastFrameMs: number | null;
	lastPositionSec: number;
};

export type Effect = "play" | "pause" | "finish";

export const initialState: MachineState = {
	phase: "idle",
	hit: "silent",
	cents: 0,
	target: null,
	streakSec: 0,
	inNoteMs: 0,
	outNoteMs: 0,
	outCause: "silence",
	lastFrameMs: null,
	lastPositionSec: 0,
};

export const startState = (positionSec: number): MachineState => ({
	...initialState,
	phase: "singing",
	lastPositionSec: positionSec,
});

const classify = (cents: number, tolerance: number): Hit =>
	Math.abs(cents) <= tolerance ? "in-note" : cents > 0 ? "sharp" : "flat";

export const step = (
	state: MachineState,
	input: MachineInput,
	index: NoteIndex,
	config: EngineConfig,
): { state: MachineState; effect: Effect | null } => {
	if (state.phase === "idle" || state.phase === "done")
		return { state, effect: null };

	const frameMs =
		state.lastFrameMs === null
			? 0
			: Math.max(0, input.nowMs - state.lastFrameMs);
	const advancedSec = Math.max(0, input.positionSec - state.lastPositionSec);
	const lookupSec = input.positionSec - config.latencyCompensationMs / 1000;

	const base: MachineState = {
		...state,
		lastFrameMs: input.nowMs,
		lastPositionSec: input.positionSec,
	};

	if (index.length > 0 && lookupSec > index.endTime) {
		return {
			state: { ...base, phase: "done", hit: "silent", target: null, cents: 0 },
			effect: "finish",
		};
	}

	const target = index.at(lookupSec);

	if (!target) {
		return {
			state: {
				...base,
				phase: "singing",
				hit: "rest",
				cents: 0,
				target: null,
				inNoteMs: 0,
				outNoteMs: 0,
				streakSec: state.streakSec + advancedSec,
			},
			effect: state.phase === "paused_out_of_note" ? "play" : null,
		};
	}

	const voiced = input.freqHz > 0 && input.confidence >= config.minConfidence;

	if (!voiced) {
		const outNoteMs =
			state.outCause === "silence" ? state.outNoteMs + frameMs : frameMs;
		const shouldPause =
			state.phase === "singing" &&
			outNoteMs >= config.exitHoldMs + config.unvoicedGraceMs;
		return {
			state: {
				...base,
				phase: shouldPause ? "paused_out_of_note" : state.phase,
				hit: "silent",
				cents: 0,
				target,
				inNoteMs: 0,
				outNoteMs,
				outCause: "silence",
				streakSec: shouldPause ? 0 : state.streakSec + advancedSec,
			},
			effect: shouldPause ? "pause" : null,
		};
	}

	const corrected = correctOctave(input.freqHz, target.pitch.freq_hz);
	const cents = centsBetween(corrected, target.pitch.freq_hz);
	const gate =
		state.phase === "paused_out_of_note" ? config.enterCents : config.exitCents;
	const satisfied = Math.abs(cents) <= gate;
	const hit = classify(cents, config.enterCents);

	if (satisfied) {
		const inNoteMs = state.inNoteMs + frameMs;
		const shouldResume =
			state.phase === "paused_out_of_note" && inNoteMs >= config.enterHoldMs;
		return {
			state: {
				...base,
				phase: shouldResume ? "singing" : state.phase,
				hit,
				cents,
				target,
				inNoteMs,
				outNoteMs: 0,
				streakSec:
					state.phase === "singing" ? state.streakSec + advancedSec : 0,
			},
			effect: shouldResume ? "play" : null,
		};
	}

	const outNoteMs =
		state.outCause === "pitch" ? state.outNoteMs + frameMs : frameMs;
	const shouldPause =
		state.phase === "singing" && outNoteMs >= config.exitHoldMs;
	return {
		state: {
			...base,
			phase: shouldPause ? "paused_out_of_note" : state.phase,
			hit,
			cents,
			target,
			inNoteMs: 0,
			outNoteMs,
			outCause: "pitch",
			streakSec: shouldPause ? 0 : state.streakSec + advancedSec,
		},
		effect: shouldPause ? "pause" : null,
	};
};
