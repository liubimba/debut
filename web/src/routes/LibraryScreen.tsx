import { AlertTriangle, ChevronRight, Music, Plus } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router";
import { api } from "../api";
import type { Song } from "../api/types";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { Skeleton } from "../components/ui/Skeleton";
import { formatTime } from "../components/ui/Timeline";

type Phase = "loading" | "ready" | "empty" | "error";

const SongRow = ({ song }: { song: Song }) => {
	const busy = song.status !== "ready";

	const inner = (
		<>
			<span className="flex size-11 shrink-0 items-center justify-center rounded-md bg-surface-2 text-text-muted">
				<Music size={18} strokeWidth={1.75} aria-hidden />
			</span>
			<span className="min-w-0 flex-1">
				<span className="block truncate font-medium text-text">
					{song.title}
				</span>
				<span className="numeric mt-0.5 block text-sm text-text-muted">
					{busy ? "Getting it ready…" : formatTime(song.duration_sec)}
				</span>
			</span>
			{busy ? null : (
				<ChevronRight
					size={18}
					strokeWidth={1.75}
					className="shrink-0 text-text-muted"
					aria-hidden
				/>
			)}
		</>
	);

	const shell = "flex items-center gap-4 rounded-md border border-border p-4";

	if (busy) return <div className={`${shell} opacity-60`}>{inner}</div>;

	return (
		<Link
			to={`/song/${song.id}`}
			className={`${shell} transition-colors duration-150 hover:bg-surface`}
		>
			{inner}
		</Link>
	);
};

const PROCESSING_EXAMPLE: Song = {
	id: "night-drive",
	title: "Night Drive",
	artist: "Imported",
	duration_sec: 214,
	status: "processing",
	stem_id: null,
	notes: null,
};

export const LibraryScreen = () => {
	const [params] = useSearchParams();
	const forced = params.get("state") as Phase | null;
	const [songs, setSongs] = useState<Song[]>([]);
	const [phase, setPhase] = useState<Phase>("loading");

	useEffect(() => {
		if (forced) {
			setPhase(forced);
			if (forced !== "ready") return;
		}
		api
			.listSongs()
			.then((list) => {
				setSongs(list);
				if (!forced) setPhase(list.length === 0 ? "empty" : "ready");
			})
			.catch(() => setPhase("error"));
	}, [forced]);

	return (
		<section className="mx-auto flex w-full max-w-2xl flex-col gap-6">
			<header>
				<h1 className="font-semibold text-2xl tracking-tight">My songs</h1>
				<p className="mt-2 text-sm text-text-muted">
					Pick a song and sing it. Debut plays the backing track and listens to
					whether you are on the note.
				</p>
			</header>

			{phase === "loading" ? (
				<div className="flex flex-col gap-3">
					{["a", "b", "c"].map((key) => (
						<Skeleton key={key} className="h-[76px] w-full rounded-md" />
					))}
				</div>
			) : null}

			{phase === "error" ? (
				<EmptyState
					icon={AlertTriangle}
					title="Debut cannot reach its engine"
					body="The heavy work runs on a program on your computer. Check that it is running, then try again."
					action={
						<Button variant="secondary" onClick={() => setPhase("loading")}>
							Try again
						</Button>
					}
				/>
			) : null}

			{phase === "empty" ? (
				<EmptyState
					icon={Music}
					title="No songs yet"
					body="Add an audio file. Debut removes the original vocal so you can sing the part yourself."
					action={
						<Link
							to="/import"
							className="inline-flex h-11 items-center gap-2 rounded-md bg-accent px-5 text-on-accent text-sm"
						>
							<Plus size={16} strokeWidth={2} aria-hidden />
							Add a song
						</Link>
					}
				/>
			) : null}

			{phase === "ready" ? (
				<>
					<div className="flex flex-col gap-3">
						{songs.map((song) => (
							<SongRow key={song.id} song={song} />
						))}
						<SongRow song={PROCESSING_EXAMPLE} />
					</div>
					<Link
						to="/import"
						className="inline-flex h-11 w-fit items-center gap-2 rounded-md border border-border px-5 text-sm text-text transition-colors duration-150 hover:bg-surface"
					>
						<Plus size={16} strokeWidth={2} aria-hidden />
						Add a song
					</Link>
				</>
			) : null}
		</section>
	);
};
