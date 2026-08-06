import { Check, Headphones, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { usePeaks } from "../components/practice/usePeaks";
import { Button } from "../components/ui/Button";
import { LevelMeter } from "../components/ui/LevelMeter";
import { formatTime, Timeline } from "../components/ui/Timeline";
import { PlayButton, RecordButton } from "../components/ui/Transport";
import { songById } from "../fixtures/songs";

type Stage = "ready" | "count-in" | "recording" | "recorded" | "sending";

const COUNT_IN = 3;

export const RecordScreen = () => {
	const { songId = "aurora-lines" } = useParams();
	const navigate = useNavigate();
	const song = useMemo(() => songById(songId), [songId]);
	const peaks = usePeaks(song.notes, song.durationSec);

	const [stage, setStage] = useState<Stage>("ready");
	const [count, setCount] = useState(COUNT_IN);
	const [position, setPosition] = useState(0);
	const [level, setLevel] = useState(0.05);
	const [playing, setPlaying] = useState(false);
	const timer = useRef<number | null>(null);

	useEffect(() => {
		if (stage !== "count-in") return;
		if (count === 0) {
			setStage("recording");
			setPosition(0);
			return;
		}
		const id = window.setTimeout(() => setCount((value) => value - 1), 900);
		return () => window.clearTimeout(id);
	}, [count, stage]);

	useEffect(() => {
		if (stage !== "recording") return;
		const id = window.setInterval(() => {
			setPosition((value) => {
				if (value >= song.durationSec) {
					window.clearInterval(id);
					setStage("recorded");
					return song.durationSec;
				}
				return value + 0.1;
			});
			setLevel(0.3 + Math.random() * 0.45);
		}, 100);
		timer.current = id;
		return () => window.clearInterval(id);
	}, [song.durationSec, stage]);

	const analyse = () => {
		setStage("sending");
		window.setTimeout(() => navigate(`/song/${song.id}/feedback`), 700);
	};

	return (
		<section className="flex min-h-dvh flex-col">
			<header className="mx-auto flex w-full max-w-lg items-center justify-between px-5 pt-[calc(1.25rem+env(safe-area-inset-top))] pb-4 md:px-0">
				<div className="min-w-0">
					<p className="text-sm text-text-muted">Record a take</p>
					<p className="truncate font-medium text-lg">{song.title}</p>
				</div>
				<Link
					to={`/song/${song.id}`}
					aria-label="Leave"
					className="hit-44 flex size-9 shrink-0 items-center justify-center rounded-md text-text-muted transition-colors duration-150 hover:text-text"
				>
					<X size={20} strokeWidth={1.75} aria-hidden />
				</Link>
			</header>

			<div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-5 pb-8 md:px-0">
				{stage === "ready" ? (
					<>
						<div>
							<h1 className="font-semibold text-xl tracking-tight">
								Sing the whole song once
							</h1>
							<p className="mt-2 text-sm text-text-muted">
								The backing track plays and does not stop, whatever happens.
								Afterwards Debut tells you what to work on.
							</p>
						</div>

						<div className="flex items-start gap-3 rounded-md bg-surface p-4">
							<Headphones
								size={18}
								strokeWidth={1.75}
								className="mt-0.5 shrink-0 text-text-muted"
								aria-hidden
							/>
							<p className="text-sm text-text-muted">
								Headphones on, or the recording will contain the backing track
								as well as you.
							</p>
						</div>

						<LevelMeter
							level={level}
							hint="Say something to check the level."
						/>
					</>
				) : null}

				{stage === "count-in" ? (
					<div className="flex flex-1 flex-col items-center justify-center gap-3">
						<p className="text-sm text-text-muted">Get ready</p>
						<p className="numeric font-semibold text-3xl">{count}</p>
					</div>
				) : null}

				{stage === "recording" ||
				stage === "recorded" ||
				stage === "sending" ? (
					<>
						<div className="flex items-center gap-3">
							{stage === "recording" ? (
								<>
									<span className="size-2.5 animate-pulse rounded-pill bg-error motion-reduce:animate-none" />
									<span className="font-medium text-error-text">Recording</span>
								</>
							) : (
								<>
									<Check
										size={18}
										strokeWidth={2.5}
										className="text-accent-text"
										aria-hidden
									/>
									<span className="font-medium text-text">Take recorded</span>
								</>
							)}
							<span className="numeric ml-auto text-sm text-text-muted">
								{formatTime(position)} / {formatTime(song.durationSec)}
							</span>
						</div>

						<Timeline
							peaks={peaks}
							duration={song.durationSec}
							position={position}
							region={null}
						/>

						{stage === "recording" ? <LevelMeter level={level} /> : null}
					</>
				) : null}

				<div className="mt-auto flex flex-col items-center gap-4 pb-[env(safe-area-inset-bottom)]">
					{stage === "ready" ? (
						<RecordButton
							recording={false}
							onToggle={() => {
								setCount(COUNT_IN);
								setStage("count-in");
							}}
						/>
					) : null}

					{stage === "recording" ? (
						<RecordButton
							recording
							onToggle={() => {
								if (timer.current) window.clearInterval(timer.current);
								setStage("recorded");
							}}
						/>
					) : null}

					{stage === "recorded" || stage === "sending" ? (
						<>
							<PlayButton
								playing={playing}
								onToggle={() => setPlaying(!playing)}
								label="Listen to your take"
							/>
							<Button
								variant="primary"
								size="hero"
								disabled={stage === "sending"}
								onClick={analyse}
							>
								{stage === "sending" ? "Sending…" : "Tell me how it went"}
							</Button>
							<button
								type="button"
								onClick={() => {
									setPosition(0);
									setStage("ready");
								}}
								className="text-sm text-text-muted underline underline-offset-4 hover:text-text"
							>
								Record it again instead
							</button>
						</>
					) : null}
				</div>
			</div>
		</section>
	);
};
