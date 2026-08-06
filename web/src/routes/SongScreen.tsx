import { ArrowLeft, ChevronDown } from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router";
import { usePeaks } from "../components/practice/usePeaks";
import { Card, CardLabel } from "../components/ui/Card";
import { Slider } from "../components/ui/Slider";
import { formatTime, type Region, Timeline } from "../components/ui/Timeline";
import { songById } from "../fixtures/songs";
import { PianoRollCanvas } from "../pianoroll/PianoRollCanvas";
import {
	DEFAULT_ROLL_OPTIONS,
	type RollSources,
} from "../pianoroll/PianoRollRenderer";
import { TraceRing } from "../pianoroll/TraceRing";

export const SongScreen = () => {
	const { songId = "aurora-lines" } = useParams();
	const song = useMemo(() => songById(songId), [songId]);
	const peaks = usePeaks(song.notes, song.durationSec);

	const [more, setMore] = useState(false);
	const [region, setRegion] = useState<Region | null>(null);
	const [guideVolume, setGuideVolume] = useState(0);
	const traceRef = useRef(new TraceRing(64));

	const overview = useMemo(
		() => ({
			...DEFAULT_ROLL_OPTIONS,
			visibleSeconds: song.durationSec,
			playheadFraction: 0,
			scroll: "track" as const,
			showPlayhead: false,
		}),
		[song.durationSec],
	);

	const sources: RollSources = {
		notes: () => song.notes,
		positionSec: () => region?.start ?? 0,
		frames: () => null,
		trace: () => traceRef.current,
		region: () => region,
		minConfidence: () => 0.5,
		running: () => false,
		onNote: () => false,
	};

	const target = region
		? `${formatTime(region.start)} to ${formatTime(region.end)}`
		: "the whole song";

	return (
		<section className="mx-auto flex w-full max-w-3xl flex-col gap-6 xl:max-w-5xl">
			<Link
				to="/"
				className="inline-flex w-fit items-center gap-2 text-sm text-text-muted transition-colors duration-150 hover:text-text"
			>
				<ArrowLeft size={16} strokeWidth={1.75} aria-hidden />
				My songs
			</Link>

			<header>
				<h1 className="font-semibold text-2xl tracking-tight">{song.title}</h1>
				<p className="numeric mt-1 text-sm text-text-muted">
					{formatTime(song.durationSec)}
				</p>
			</header>

			<div>
				<p className="mb-2 text-sm text-text-muted">
					This is the melody you will sing. Higher on the screen means a higher
					note.
				</p>
				<div className="h-40 overflow-hidden rounded-md border border-border md:h-48 xl:h-64">
					<PianoRollCanvas
						sources={sources}
						options={overview}
						revision={`${songId}-${region?.start.toFixed(1) ?? "all"}`}
						label={`The melody of ${song.title}`}
					/>
				</div>
			</div>

			<Link
				to={`/song/${song.id}/singalong`}
				className="flex h-14 w-full items-center justify-center rounded-md bg-accent font-medium text-base text-on-accent transition-opacity duration-150 hover:opacity-90"
			>
				Sing along
			</Link>

			<p className="-mt-3 text-center text-sm text-text-muted">
				The backing track plays while you are on the note and stops when you are
				not. You will sing {target}.
			</p>

			<div className="flex flex-col items-center gap-1">
				<Link
					to={`/song/${song.id}/record`}
					className="text-sm text-text-muted underline underline-offset-4 transition-colors duration-150 hover:text-text"
				>
					Or record a take and get feedback
				</Link>
			</div>

			<div className="border-border border-t pt-4">
				<button
					type="button"
					onClick={() => setMore(!more)}
					aria-expanded={more}
					className="flex w-full items-center justify-between text-sm text-text-muted transition-colors duration-150 hover:text-text"
				>
					More options
					<ChevronDown
						size={16}
						strokeWidth={1.75}
						className={`transition-transform duration-200 ${more ? "rotate-180" : ""}`}
						aria-hidden
					/>
				</button>

				{more ? (
					<div className="mt-4 flex flex-col gap-5">
						<Card className="flex flex-col gap-3">
							<CardLabel>Sing only part of the song</CardLabel>
							<p className="text-sm text-text-muted">
								Drag across the picture below to pick a stretch. Leave it alone
								to sing the whole song.
							</p>
							<Timeline
								peaks={peaks}
								duration={song.durationSec}
								position={region?.start ?? 0}
								region={region}
								onRegionChange={setRegion}
							/>
							{region ? (
								<button
									type="button"
									onClick={() => setRegion(null)}
									className="w-fit text-sm text-text-muted underline underline-offset-4 hover:text-text"
								>
									Use the whole song instead
								</button>
							) : null}
						</Card>

						<Card className="flex flex-col gap-3">
							<CardLabel>Guide vocal</CardLabel>
							<Slider
								label="How loud the original singer is"
								value={guideVolume}
								min={0}
								max={100}
								unit="%"
								hint="Off by default — that is the point. Turn it up if you want to hear the tune first."
								onChange={setGuideVolume}
							/>
						</Card>
					</div>
				) : null}
			</div>
		</section>
	);
};
