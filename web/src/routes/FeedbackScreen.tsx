import { Check, ChevronDown, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import { api, pollJob } from "../api";
import type { FeedbackResult } from "../api/types";
import { usePeaks } from "../components/practice/usePeaks";
import { Card } from "../components/ui/Card";
import { Skeleton } from "../components/ui/Skeleton";
import { Timeline } from "../components/ui/Timeline";
import { PlayButton } from "../components/ui/Transport";
import { songById } from "../fixtures/songs";

type Phase = "working" | "ready" | "error";

export const FeedbackScreen = () => {
	const { songId = "aurora-lines" } = useParams();
	const song = useMemo(() => songById(songId), [songId]);
	const peaks = usePeaks(song.notes, song.durationSec);

	const [phase, setPhase] = useState<Phase>("working");
	const [result, setResult] = useState<FeedbackResult | null>(null);
	const [playing, setPlaying] = useState(false);
	const [showAll, setShowAll] = useState(false);

	useEffect(() => {
		let cancelled = false;
		api
			.analyzeTake(song.id, 0, song.durationSec)
			.then((job) => pollJob<FeedbackResult>(api, job.id))
			.then((job) => {
				if (cancelled) return;
				if (job.state === "FAILED" || job.result === null) {
					setPhase("error");
					return;
				}
				setResult(job.result);
				setPhase("ready");
			})
			.catch(() => {
				if (!cancelled) setPhase("error");
			});
		return () => {
			cancelled = true;
		};
	}, [song]);

	const workOn = result?.metrics.filter((m) => m.verdict === "work-on") ?? [];
	const good = result?.metrics.filter((m) => m.verdict === "good") ?? [];
	const top = workOn.slice(0, 3);

	return (
		<section className="flex min-h-dvh flex-col">
			<header className="mx-auto flex w-full max-w-2xl items-center justify-between px-5 pt-[calc(1.25rem+env(safe-area-inset-top))] pb-4 md:px-0">
				<div className="min-w-0">
					<p className="text-sm text-text-muted">How it went</p>
					<p className="truncate font-medium text-lg">{song.title}</p>
				</div>
				<Link
					to={`/song/${song.id}`}
					aria-label="Close"
					className="hit-44 flex size-9 shrink-0 items-center justify-center rounded-md text-text-muted transition-colors duration-150 hover:text-text"
				>
					<X size={20} strokeWidth={1.75} aria-hidden />
				</Link>
			</header>

			<div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-5 pb-8 md:px-0">
				{phase === "working" ? (
					<>
						<p className="text-sm text-text-muted">
							Listening to your take. This takes a few seconds.
						</p>
						<Skeleton className="h-8 w-4/5" />
						<div className="flex flex-col gap-4">
							{["a", "b", "c"].map((key) => (
								<Card key={key} className="flex flex-col gap-3">
									<Skeleton className="h-4 w-40" />
									<Skeleton className="h-3 w-full" />
									<Skeleton className="h-3 w-2/3" />
								</Card>
							))}
						</div>
					</>
				) : null}

				{phase === "error" ? (
					<Card>
						<p className="font-medium text-error-text">
							Debut could not analyse the take
						</p>
						<p className="mt-2 text-sm text-text-muted">
							The analysis engine is not answering. Your recording is safe — try
							again in a moment.
						</p>
					</Card>
				) : null}

				{phase === "ready" && result ? (
					<>
						<h1 className="font-semibold text-2xl tracking-tight">
							{result.headline}
						</h1>

						<Card className="flex flex-col gap-4">
							<p className="text-sm text-text-muted">
								This is the take Debut listened to.
							</p>
							<Timeline
								peaks={peaks}
								duration={song.durationSec}
								position={0}
								region={null}
							/>
							<PlayButton
								playing={playing}
								onToggle={() => setPlaying(!playing)}
								label="Listen to your take"
							/>
						</Card>

						<div>
							<h2 className="font-medium text-lg">
								{top.length === 0
									? "Nothing to fix — sing it again for fun"
									: `Work on ${top.length === 1 ? "this" : `these ${top.length}`}`}
							</h2>
							<ol className="mt-3 flex flex-col gap-3">
								{top.map((metric, index) => (
									<li key={metric.key}>
										<Card className="flex gap-4">
											<span className="numeric flex size-7 shrink-0 items-center justify-center rounded-pill bg-surface-2 font-medium text-sm">
												{index + 1}
											</span>
											<div className="min-w-0">
												<p className="font-medium text-text">{metric.plain}</p>
												{metric.advice ? (
													<p className="mt-1.5 text-sm text-text-muted">
														{metric.advice}
													</p>
												) : null}
											</div>
										</Card>
									</li>
								))}
							</ol>
						</div>

						{good.length > 0 ? (
							<div>
								<h2 className="font-medium text-lg">This already works</h2>
								<ul className="mt-3 flex flex-col gap-2">
									{good.map((metric) => (
										<li
											key={metric.key}
											className="flex items-start gap-3 text-sm text-text-muted"
										>
											<Check
												size={16}
												strokeWidth={2.5}
												className="mt-0.5 shrink-0 text-accent-text"
												aria-hidden
											/>
											{metric.plain}
										</li>
									))}
								</ul>
							</div>
						) : null}

						<div className="border-border border-t pt-4">
							<button
								type="button"
								onClick={() => setShowAll(!showAll)}
								aria-expanded={showAll}
								className="flex w-full items-center justify-between text-sm text-text-muted transition-colors duration-150 hover:text-text"
							>
								All the numbers
								<ChevronDown
									size={16}
									strokeWidth={1.75}
									className={`transition-transform duration-200 ${showAll ? "rotate-180" : ""}`}
									aria-hidden
								/>
							</button>
							{showAll ? (
								<dl className="mt-4 flex flex-col gap-3">
									{result.metrics.map((metric) => (
										<div
											key={metric.key}
											className="flex items-baseline justify-between gap-4 border-border border-b pb-3 last:border-0"
										>
											<dt className="text-sm text-text">{metric.label}</dt>
											<dd
												className={`numeric text-sm ${
													metric.verdict === "work-on"
														? "text-error-text"
														: "text-text-muted"
												}`}
											>
												{metric.value}
											</dd>
										</div>
									))}
								</dl>
							) : null}
						</div>
					</>
				) : null}
			</div>

			{phase === "ready" ? (
				<footer className="mx-auto flex w-full max-w-2xl flex-col gap-3 border-border border-t px-5 pt-4 pb-[calc(1.25rem+env(safe-area-inset-bottom))] md:flex-row md:px-0">
					<Link
						to={`/song/${song.id}/record`}
						className="flex h-14 flex-1 items-center justify-center rounded-md bg-accent font-medium text-on-accent transition-opacity duration-150 hover:opacity-90"
					>
						Sing it again
					</Link>
					<Link
						to={`/song/${song.id}`}
						className="flex h-14 flex-1 items-center justify-center rounded-md border border-border text-text transition-colors duration-150 hover:bg-surface"
					>
						Back to the song
					</Link>
				</footer>
			) : null}
		</section>
	);
};
