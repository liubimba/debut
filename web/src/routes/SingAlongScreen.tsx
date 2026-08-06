import { Check, Headphones, Mic, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router";
import { api } from "../api";
import { DEFAULT_CONFIG } from "../audio/config";
import { midiToName } from "../audio/dsp/notes";
import { useSingAlong } from "../audio/react/useSingAlong";
import { Button } from "../components/ui/Button";
import { LevelMeter } from "../components/ui/LevelMeter";
import { type HitState, PitchIndicator } from "../components/ui/PitchIndicator";
import { songById } from "../fixtures/songs";
import { PianoRollCanvas } from "../pianoroll/PianoRollCanvas";
import {
	DEFAULT_ROLL_OPTIONS,
	type RollSources,
} from "../pianoroll/PianoRollRenderer";
import { TraceRing } from "../pianoroll/TraceRing";

type Screen = "ready" | "count-in" | "playing";

const hitToIndicator = (hit: string): HitState => {
	if (hit === "in-note") return "in-note";
	if (hit === "sharp") return "sharp";
	if (hit === "flat") return "flat";
	return "silent";
};

const Step = ({
	done,
	title,
	children,
}: {
	done: boolean;
	title: string;
	children?: React.ReactNode;
}) => (
	<li className="flex gap-4">
		<span
			className={`mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-pill border text-xs ${
				done
					? "border-accent bg-accent text-on-accent"
					: "border-border text-text-muted"
			}`}
		>
			{done ? <Check size={13} strokeWidth={3} aria-hidden /> : null}
		</span>
		<div className="min-w-0 flex-1">
			<p className="font-medium text-text">{title}</p>
			{children}
		</div>
	</li>
);

export const SingAlongScreen = () => {
	const { songId = "aurora-lines" } = useParams();
	const song = useMemo(() => songById(songId), [songId]);
	const { engine, phase, error, micOpen, readout, openMic, load, start, stop } =
		useSingAlong();

	const [screen, setScreen] = useState<Screen>("ready");
	const [headphones, setHeadphones] = useState(false);
	const [count, setCount] = useState(3);
	const traceRef = useRef(new TraceRing(4096));

	useEffect(() => {
		if (screen !== "count-in") return;
		if (count === 0) {
			setScreen("playing");
			start();
			return;
		}
		const timer = window.setTimeout(() => setCount((value) => value - 1), 900);
		return () => window.clearTimeout(timer);
	}, [count, screen, start]);

	const begin = useCallback(async () => {
		traceRef.current.clear();
		const stems = await api.stems(song.id);
		await load(
			song.notes,
			stems
				.filter((stem) => stem.name !== "vocals.wav")
				.map((stem) => ({ id: stem.name, url: stem.url })),
		);
		setCount(3);
		setScreen("count-in");
	}, [load, song]);

	const leave = useCallback(async () => {
		await stop();
		setScreen("ready");
	}, [stop]);

	const sources: RollSources = {
		notes: () => song.notes,
		positionSec: () => engine.positionSec(),
		frames: () => engine.frames,
		trace: () => traceRef.current,
		region: () => null,
		minConfidence: () => engine.settings.minConfidence,
		running: () => phase === "singing" || phase === "paused_out_of_note",
		onNote: () => engine.state.hit === "in-note" || engine.state.hit === "rest",
	};

	const noteName =
		readout.noteMidi === null ? "—" : midiToName(readout.noteMidi);

	return (
		<section className="flex h-dvh flex-col overflow-hidden">
			<header className="flex items-center justify-between px-5 pt-[calc(1.25rem+env(safe-area-inset-top))] pb-4 md:px-10">
				<div className="min-w-0">
					<p className="text-sm text-text-muted">Sing along</p>
					<p className="truncate font-medium text-lg">{song.title}</p>
				</div>
				<Link
					to={`/song/${song.id}`}
					onClick={leave}
					aria-label="Leave"
					className="hit-44 flex size-9 shrink-0 items-center justify-center rounded-md text-text-muted transition-colors duration-150 hover:text-text"
				>
					<X size={20} strokeWidth={1.75} aria-hidden />
				</Link>
			</header>

			{screen === "ready" ? (
				<div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-7 overflow-y-auto px-5 pb-8 md:px-0">
					<div>
						<h1 className="font-semibold text-xl tracking-tight">
							Two things before you start
						</h1>
						<p className="mt-2 text-sm text-text-muted">
							Then the song plays and you sing along with it.
						</p>
					</div>

					<ol className="flex flex-col gap-7">
						<Step done={headphones} title="Put your headphones on">
							<p className="mt-1 text-sm text-text-muted">
								Through speakers the microphone hears the music as well as you,
								and Debut cannot tell you apart from the song.
							</p>
							{headphones ? null : (
								<Button
									variant="secondary"
									className="mt-3"
									onClick={() => setHeadphones(true)}
								>
									<Headphones size={16} strokeWidth={1.75} aria-hidden />
									They are on
								</Button>
							)}
						</Step>

						<Step done={readout.heardVoice} title="Let Debut hear you">
							<p className="mt-1 text-sm text-text-muted">
								{micOpen
									? "Sing any note — aaa — until the bar moves."
									: "Debut listens to your voice on this device. Nothing is sent anywhere."}
							</p>
							{micOpen ? (
								<div className="mt-3">
									<LevelMeter level={readout.level} />
								</div>
							) : (
								<Button variant="secondary" className="mt-3" onClick={openMic}>
									<Mic size={16} strokeWidth={1.75} aria-hidden />
									Turn on the microphone
								</Button>
							)}
						</Step>
					</ol>

					{error ? (
						<div className="rounded-md border border-error/40 bg-surface p-4">
							<p className="font-medium text-error-text text-sm">
								Debut cannot use the microphone
							</p>
							<p className="mt-1 text-sm text-text-muted">{error}</p>
						</div>
					) : null}

					<div className="mt-auto flex flex-col gap-3 pb-[env(safe-area-inset-bottom)]">
						<p className="rounded-md bg-surface p-4 text-sm text-text-muted">
							<span className="text-text">How it works.</span> The music keeps
							playing while you are on the right note. Drift off and it stops
							until you find the note again.
						</p>
						<Button
							variant="primary"
							size="hero"
							data-testid="start"
							disabled={!headphones || !readout.heardVoice}
							onClick={begin}
						>
							{headphones && readout.heardVoice
								? "Start the song"
								: "Finish both steps first"}
						</Button>
					</div>
				</div>
			) : null}

			{screen === "count-in" ? (
				<div className="flex flex-1 flex-col items-center justify-center gap-4">
					<p className="text-sm text-text-muted">Get ready</p>
					<p className="numeric font-semibold text-3xl">{count}</p>
					<p className="text-sm text-text-muted">
						First note is {midiToName(song.notes[0]?.pitch.midi ?? 69)}
					</p>
				</div>
			) : null}

			{screen === "playing" ? (
				<>
					<div className="mx-5 min-h-0 flex-1 overflow-hidden rounded-md border border-border md:mx-10">
						<PianoRollCanvas
							sources={sources}
							options={DEFAULT_ROLL_OPTIONS}
							revision={`${phase}-${song.id}`}
							label={`The melody of ${song.title}, with your voice drawn over it`}
						/>
					</div>

					<div className="flex shrink-0 flex-col items-center gap-5 px-5 py-6 md:px-10">
						<span data-testid="phase" className="sr-only">
							{phase}
						</span>

						{phase === "paused_out_of_note" ? (
							<div className="w-full max-w-sm rounded-md bg-surface p-4 text-center">
								<p className="font-medium text-text">The music stopped</p>
								<p className="mt-1 text-sm text-text-muted">
									You drifted off the note. Sing {noteName} again and it will
									carry on.
								</p>
							</div>
						) : null}

						{phase === "done" ? (
							<div className="w-full max-w-sm text-center">
								<p className="font-semibold text-xl">That was the whole song</p>
								<p className="mt-2 text-sm text-text-muted">
									You held the note for {readout.streakSec.toFixed(0)} seconds
									in a row at your best.
								</p>
							</div>
						) : (
							<PitchIndicator
								noteName={noteName}
								cents={readout.cents}
								state={hitToIndicator(readout.hit)}
								tolerance={engine.settings.enterCents}
							/>
						)}
					</div>

					<footer className="flex justify-center gap-3 border-border border-t px-5 pt-4 pb-[calc(1.25rem+env(safe-area-inset-bottom))] md:px-10">
						{phase === "done" ? (
							<>
								<Button variant="secondary" onClick={() => setScreen("ready")}>
									Sing it again
								</Button>
								<Link
									to={`/song/${song.id}/record`}
									className="inline-flex h-11 items-center rounded-md bg-accent px-5 text-on-accent text-sm"
								>
									Record a take
								</Link>
							</>
						) : (
							<Button variant="secondary" onClick={leave}>
								Stop
							</Button>
						)}
					</footer>
				</>
			) : null}
		</section>
	);
};

export const SING_ALONG_TOLERANCE = DEFAULT_CONFIG.enterCents;
