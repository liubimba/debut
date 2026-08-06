import { useCallback, useEffect, useRef, useState } from "react";
import type { EngineConfig } from "../config";
import { type EngineStems, PitchEngine } from "../PitchEngine";
import type { MachineState } from "../singalong/SingAlongMachine";
import { initialState } from "../singalong/SingAlongMachine";
import type { Note } from "../types";

const READOUT_HZ = 15;

export type Readout = {
	hit: MachineState["hit"];
	cents: number;
	noteMidi: number | null;
	streakSec: number;
	level: number;
	heardVoice: boolean;
};

const EMPTY: Readout = {
	hit: "silent",
	cents: 0,
	noteMidi: null,
	streakSec: 0,
	level: 0,
	heardVoice: false,
};

export const useSingAlong = () => {
	const engineRef = useRef<PitchEngine | null>(null);
	if (engineRef.current === null) engineRef.current = new PitchEngine();
	const engine = engineRef.current;

	const [phase, setPhase] = useState<MachineState["phase"]>(initialState.phase);
	const [error, setError] = useState<string | null>(null);
	const [micOpen, setMicOpen] = useState(false);
	const [readout, setReadout] = useState<Readout>(EMPTY);
	const heardRef = useRef(false);

	useEffect(() => {
		const unsubscribe = engine.subscribe((state) => setPhase(state.phase));
		return () => {
			unsubscribe();
		};
	}, [engine]);

	useEffect(() => {
		if (!micOpen) return;
		const timer = setInterval(() => {
			const state = engine.state;
			const level = engine.inputLevel();
			if (level > 0.25) heardRef.current = true;
			setReadout({
				hit: state.hit,
				cents: state.cents,
				noteMidi: state.target?.pitch.midi ?? null,
				streakSec: state.streakSec,
				level,
				heardVoice: heardRef.current,
			});
		}, 1000 / READOUT_HZ);
		return () => clearInterval(timer);
	}, [engine, micOpen]);

	useEffect(() => () => void engine.stop(), [engine]);

	const openMic = useCallback(async () => {
		setError(null);
		try {
			await engine.listen();
			setMicOpen(true);
		} catch (cause) {
			setError(cause instanceof Error ? cause.message : String(cause));
		}
	}, [engine]);

	const load = useCallback(
		async (
			notes: Note[],
			stems: EngineStems,
			config?: Partial<EngineConfig>,
		) => {
			setError(null);
			try {
				if (config) engine.configure(config);
				await engine.loadSong(notes, stems);
				setMicOpen(true);
			} catch (cause) {
				setError(cause instanceof Error ? cause.message : String(cause));
			}
		},
		[engine],
	);

	const start = useCallback(() => engine.startSinging(), [engine]);

	const stop = useCallback(async () => {
		await engine.stop();
		heardRef.current = false;
		setMicOpen(false);
		setReadout(EMPTY);
	}, [engine]);

	const configure = useCallback(
		(patch: Partial<EngineConfig>) => engine.configure(patch),
		[engine],
	);

	return {
		engine,
		phase,
		error,
		micOpen,
		readout,
		openMic,
		load,
		start,
		stop,
		configure,
	};
};
