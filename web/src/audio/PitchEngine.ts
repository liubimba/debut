import { type MicHandle, openMicrophone } from "./capture/MicInput";
import { clampConfig, DEFAULT_CONFIG, type EngineConfig } from "./config";
import { FrameBus } from "./FrameBus";
import { NoteIndex } from "./singalong/NoteIndex";
import {
	type Effect,
	initialState,
	type MachineState,
	startState,
	step,
} from "./singalong/SingAlongMachine";
import { BackingTrack } from "./transport/BackingTrack";
import type { Note } from "./types";

export type EngineStems = { id: string; url: string }[];

export class PitchEngine {
	readonly frames = new FrameBus(1024);
	private config: EngineConfig = DEFAULT_CONFIG;
	private index = new NoteIndex([]);
	private machine: MachineState = initialState;
	private mic: MicHandle | null = null;
	private track: BackingTrack | null = null;
	private listeners = new Set<(state: MachineState) => void>();
	private lastNotifiedPhase: MachineState["phase"] = "idle";
	private level = 0;

	get state() {
		return this.machine;
	}

	get settings() {
		return this.config;
	}

	get micIsOpen() {
		return this.mic !== null;
	}

	inputLevel() {
		return this.level;
	}

	positionSec() {
		return this.track?.positionSec() ?? 0;
	}

	subscribe(listener: (state: MachineState) => void) {
		this.listeners.add(listener);
		return () => this.listeners.delete(listener);
	}

	configure(patch: Partial<EngineConfig>) {
		this.config = clampConfig({ ...this.config, ...patch });
	}

	async listen() {
		if (this.mic) return;
		this.mic = await openMicrophone(this.config, (frame) =>
			this.onFrame(frame),
		);
	}

	async loadSong(notes: Note[], stems: EngineStems) {
		await this.listen();
		this.index = new NoteIndex(notes);
		const context = this.mic?.context;
		if (!context) throw new Error("microphone is not open");
		this.track?.dispose();
		this.track = new BackingTrack(context, this.config.fadeMs);
		for (const stem of stems) await this.track.load(stem.id, stem.url);
	}

	startSinging() {
		this.index.reset();
		this.machine = startState(0);
		this.track?.seek(0);
		this.track?.play();
		this.notify(true);
	}

	async stop() {
		this.track?.dispose();
		this.track = null;
		await this.mic?.stop();
		this.mic = null;
		this.machine = initialState;
		this.frames.clear();
		this.level = 0;
		this.notify(true);
	}

	private onFrame(frame: {
		audioTime: number;
		freqHz: number;
		confidence: number;
	}) {
		this.frames.push(frame);
		this.level =
			frame.confidence > 0
				? Math.min(1, this.level * 0.7 + frame.confidence * 0.5)
				: this.level * 0.8;

		if (this.machine.phase === "idle" || this.machine.phase === "done") return;

		const result = step(
			this.machine,
			{
				nowMs: frame.audioTime * 1000,
				positionSec: this.positionSec(),
				freqHz: frame.freqHz,
				confidence: frame.confidence,
			},
			this.index,
			this.config,
		);

		this.machine = result.state;
		if (result.effect) this.apply(result.effect);
		this.notify(false);
	}

	private apply(effect: Effect) {
		if (effect === "play") this.track?.play();
		if (effect === "pause" || effect === "finish") this.track?.pause();
	}

	private notify(force: boolean) {
		if (!force && this.machine.phase === this.lastNotifiedPhase) return;
		this.lastNotifiedPhase = this.machine.phase;
		for (const listener of this.listeners) listener(this.machine);
	}
}
