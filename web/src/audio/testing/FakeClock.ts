import { DEFAULT_CONFIG, type EngineConfig } from "../config";
import { midiToHz } from "../dsp/notes";
import { NoteIndex } from "../singalong/NoteIndex";
import {
	type Effect,
	type MachineState,
	startState,
	step,
} from "../singalong/SingAlongMachine";
import type { Note } from "../types";

export type Frame = {
	freqHz: number;
	confidence: number;
};

export const note = (midi: number, start: number, end: number): Note => ({
	pitch: { midi, freq_hz: midiToHz(midi), cents_offset: 0, confidence: 1 },
	start_time: start,
	end_time: end,
});

export const offBy = (midi: number, cents: number): Frame => ({
	freqHz: midiToHz(midi) * 2 ** (cents / 1200),
	confidence: 0.9,
});

export const silence: Frame = { freqHz: 0, confidence: 0 };

export class Driver {
	private state: MachineState;
	private readonly index: NoteIndex;
	readonly effects: Effect[] = [];
	nowMs = 0;
	positionSec = 0;

	constructor(
		notes: Note[],
		private readonly config: EngineConfig = DEFAULT_CONFIG,
		private readonly frameMs = 10,
	) {
		this.index = new NoteIndex(notes);
		this.state = startState(0);
	}

	get phase() {
		return this.state.phase;
	}

	get hit() {
		return this.state.hit;
	}

	get cents() {
		return this.state.cents;
	}

	get streakSec() {
		return this.state.streakSec;
	}

	get target() {
		return this.state.target;
	}

	get lastEffect() {
		return this.effects.at(-1) ?? null;
	}

	advance(durationMs: number, frame: Frame, trackRuns = true) {
		const steps = Math.round(durationMs / this.frameMs);
		for (let i = 0; i < steps; i += 1) {
			this.nowMs += this.frameMs;
			if (trackRuns && this.state.phase === "singing") {
				this.positionSec += this.frameMs / 1000;
			}
			const result = step(
				this.state,
				{
					nowMs: this.nowMs,
					positionSec: this.positionSec,
					freqHz: frame.freqHz,
					confidence: frame.confidence,
				},
				this.index,
				this.config,
			);
			this.state = result.state;
			if (result.effect) this.effects.push(result.effect);
		}
		return this;
	}

	seek(positionSec: number) {
		this.positionSec = positionSec;
		return this;
	}
}
