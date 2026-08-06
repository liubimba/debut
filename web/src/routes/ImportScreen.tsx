import { AlertTriangle, Check, Loader, Music, Upload } from "lucide-react";
import { useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { api, pollJob } from "../api";
import type { JobState } from "../api/types";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { EmptyState } from "../components/ui/EmptyState";

type StageId = "queued" | "separating" | "transcribing" | "done";

const STAGES: { id: StageId; label: string }[] = [
	{ id: "queued", label: "Waiting its turn" },
	{ id: "separating", label: "Taking the original singer out" },
	{ id: "transcribing", label: "Working out the melody" },
	{ id: "done", label: "Ready to sing" },
];

export const ImportScreen = () => {
	const navigate = useNavigate();
	const [params] = useSearchParams();
	const inputRef = useRef<HTMLInputElement>(null);

	const forced = params.get("state");
	const [fileName, setFileName] = useState<string | null>(
		forced ? "night-drive.mp3" : null,
	);
	const [stage, setStage] = useState<StageId | null>(
		forced && forced !== "error"
			? (forced as StageId)
			: forced
				? "separating"
				: null,
	);
	const [elapsed, setElapsed] = useState(0);
	const [failure, setFailure] = useState<string | null>(
		forced === "error"
			? "Debut could not read this file. It may be damaged, or in a format it does not understand."
			: null,
	);
	const [songId, setSongId] = useState<string | null>(
		forced === "done" ? "aurora-lines" : null,
	);

	const reset = () => {
		setStage(null);
		setElapsed(0);
		setFailure(null);
		setSongId(null);
		setFileName(null);
	};

	const run = async (file: File) => {
		setFileName(file.name);
		setFailure(null);
		setStage("queued");
		setElapsed(0);

		const ticker = window.setInterval(
			() => setElapsed((value) => value + 1000),
			1000,
		);

		try {
			const { song, separateJobId, transcribeJobId } =
				await api.importSong(file);
			setSongId(song.id);

			const onSeparate = (state: JobState) => {
				if (state === "RUNNING") setStage("separating");
			};
			const separate = await pollJob(api, separateJobId, {
				onState: onSeparate,
			});
			if (separate.state === "FAILED")
				throw new Error(separate.error_message ?? "Could not split the file");

			setStage("transcribing");
			const transcribe = await pollJob(api, transcribeJobId);
			if (transcribe.state === "FAILED")
				throw new Error(
					transcribe.error_message ?? "Could not read the melody",
				);

			setStage("done");
		} catch (cause) {
			setFailure(cause instanceof Error ? cause.message : String(cause));
		} finally {
			window.clearInterval(ticker);
		}
	};

	const activeIndex =
		stage === null ? -1 : STAGES.findIndex((item) => item.id === stage);

	return (
		<section className="mx-auto flex w-full max-w-xl flex-col gap-6">
			<header>
				<h1 className="font-semibold text-2xl tracking-tight">Add a song</h1>
				<p className="mt-2 text-sm text-text-muted">
					Debut takes the original singer out of the track and works out the
					melody, so you can sing the part yourself.
				</p>
			</header>

			<input
				ref={inputRef}
				type="file"
				accept="audio/*"
				className="sr-only"
				onChange={(event) => {
					const file = event.target.files?.[0];
					if (file) void run(file);
				}}
			/>

			{stage === null ? (
				<EmptyState
					icon={Upload}
					title="Choose an audio file"
					body="An MP3, WAV or FLAC from your computer. The file never leaves your machine."
					action={
						<Button variant="primary" onClick={() => inputRef.current?.click()}>
							Choose a file
						</Button>
					}
				/>
			) : (
				<Card className="flex flex-col gap-5">
					<div className="flex items-baseline justify-between gap-4">
						<p className="min-w-0 truncate font-medium text-text">{fileName}</p>
						<p className="numeric shrink-0 text-sm text-text-muted">
							{Math.floor(elapsed / 1000)}s
						</p>
					</div>

					<ol className="flex flex-col gap-3">
						{STAGES.map((item, index) => {
							const done = activeIndex > index || stage === "done";
							const active = activeIndex === index && stage !== "done";
							const failedHere = failure !== null && activeIndex === index;
							return (
								<li key={item.id} className="flex items-center gap-3">
									<span
										className={`flex size-5 shrink-0 items-center justify-center rounded-pill ${
											failedHere
												? "bg-error text-on-error"
												: done
													? "bg-accent text-on-accent"
													: "border border-border"
										}`}
									>
										{failedHere ? (
											<AlertTriangle size={11} strokeWidth={2.5} aria-hidden />
										) : done ? (
											<Check size={11} strokeWidth={3} aria-hidden />
										) : active ? (
											<Loader
												size={11}
												strokeWidth={2.5}
												className="animate-spin text-text-muted motion-reduce:animate-none"
												aria-hidden
											/>
										) : null}
									</span>
									<span
										className={`text-sm ${
											active
												? "text-text"
												: done
													? "text-text"
													: "text-text-muted"
										}`}
									>
										{item.label}
									</span>
								</li>
							);
						})}
					</ol>

					{failure === null && stage !== "done" ? (
						<div className="h-1 overflow-hidden rounded-pill bg-surface-2">
							<div className="h-full w-1/3 animate-[indeterminate_1.4s_ease-in-out_infinite] bg-accent motion-reduce:w-full" />
						</div>
					) : null}

					{failure !== null ? (
						<div className="rounded-sm bg-surface-2 p-3">
							<p className="font-medium text-error-text text-sm">
								Something went wrong
							</p>
							<p className="mt-1 text-sm text-text-muted">{failure}</p>
						</div>
					) : null}

					<div className="flex flex-wrap gap-3">
						{stage === "done" && songId ? (
							<Button
								variant="primary"
								onClick={() => navigate(`/song/${songId}`)}
							>
								<Music size={16} strokeWidth={1.75} aria-hidden />
								Sing it
							</Button>
						) : null}
						{failure !== null ? (
							<Button
								variant="secondary"
								onClick={() => inputRef.current?.click()}
							>
								Try another file
							</Button>
						) : null}
						<Button variant="quiet" onClick={reset}>
							{stage === "done" || failure !== null ? "Add another" : "Cancel"}
						</Button>
					</div>
				</Card>
			)}
		</section>
	);
};
