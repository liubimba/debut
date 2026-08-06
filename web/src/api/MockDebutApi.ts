import { SONGS, type SongFixture } from "../fixtures/songs";
import {
	renderInstrumental,
	renderReferenceVocal,
	SYNTH_SAMPLE_RATE,
} from "../fixtures/synth";
import { toObjectUrl } from "../fixtures/wav";
import type { DebutApi } from "./DebutApi";
import type {
	FeedbackResult,
	Job,
	Note,
	SeparateResult,
	Song,
	StemRef,
} from "./types";

const hex = (length: number) =>
	Array.from({ length }, () =>
		Math.floor(Math.random() * 16).toString(16),
	).join("");

const nowIso = () => new Date().toISOString().replace("Z", "");

const METRICS: FeedbackResult["metrics"] = [
	{
		key: "timing",
		label: "Coming in on time",
		value: "0.1 s late",
		fill: 0.35,
		verdict: "work-on",
		plain: "You start each line a beat behind the music.",
		advice: "Breathe in on the beat before you sing, not on the beat you sing.",
	},
	{
		key: "pitch_drift",
		label: "Landing on the note",
		value: "a little flat",
		fill: 0.55,
		verdict: "work-on",
		plain: "You sit just under most notes rather than on them.",
		advice:
			"Aim slightly higher than feels right — being flat almost always feels correct.",
	},
	{
		key: "volume",
		label: "Volume",
		value: "quiet",
		fill: 0.4,
		verdict: "work-on",
		plain: "You sing noticeably quieter than the song does.",
		advice: "Stand up and sing to the far wall of the room.",
	},
	{
		key: "pitch_accuracy",
		label: "Notes you hit",
		value: "78 out of 100",
		fill: 0.78,
		verdict: "good",
		plain: "Most notes were inside the target.",
		advice: null,
	},
	{
		key: "breaths",
		label: "Breathing",
		value: "2 extra breaths",
		fill: 0.3,
		verdict: "work-on",
		plain: "Two lines got broken in half by a breath.",
		advice:
			"Take a bigger breath before the long line instead of a small one halfway through.",
	},
	{
		key: "steadiness",
		label: "Steadiness",
		value: "steady",
		fill: 0.85,
		verdict: "good",
		plain: "Your voice holds still — no wobble worth mentioning.",
		advice: null,
	},
];

type StoredJob = Job<unknown> & { finishAt: number; produce: () => unknown };

export class MockDebutApi implements DebutApi {
	private readonly jobs = new Map<string, StoredJob>();
	private readonly stemUrls = new Map<string, StemRef[]>();
	private readonly songs: Song[];

	constructor(private readonly latencyMs = 900) {
		this.songs = SONGS.map((fixture) => ({
			id: fixture.id,
			title: fixture.title,
			artist: fixture.artist,
			duration_sec: fixture.durationSec,
			status: "ready" as const,
			stem_id: fixture.id,
			notes: fixture.notes,
		}));
	}

	private queue<Result>(produce: () => Result, extraMs = 0): Job<Result> {
		const job: StoredJob = {
			id: hex(32),
			state: "QUEUED",
			started_at: nowIso(),
			finished_at: null,
			error_message: null,
			result: null,
			finishAt: Date.now() + this.latencyMs + extraMs,
			produce,
		};
		this.jobs.set(job.id, job);
		return { ...job } as Job<Result>;
	}

	async job<Result>(id: string): Promise<Job<Result>> {
		const stored = this.jobs.get(id);
		if (!stored) throw new Error("Job not found");

		const remaining = stored.finishAt - Date.now();
		if (stored.state !== "FINISHED" && stored.state !== "FAILED") {
			if (remaining <= 0) {
				stored.state = "FINISHED";
				stored.finished_at = nowIso();
				stored.result = stored.produce();
			} else if (remaining < this.latencyMs * 0.7) {
				stored.state = "RUNNING";
			}
		}

		const { finishAt: _finishAt, produce: _produce, ...job } = stored;
		return job as Job<Result>;
	}

	async transcribe(_file: File) {
		const fixture = SONGS[0];
		return this.queue<Note[]>(() => fixture?.notes ?? []);
	}

	async separate(_file: File, _sampleRate = 44100, stemId?: string) {
		const id = stemId ?? hex(32);
		return this.queue<SeparateResult>(() => {
			this.materializeStems(id, SONGS[0]);
			return { stem_id: id, stems: ["instrumental.wav", "vocals.wav"] };
		}, 1500);
	}

	async stems(stemId: string): Promise<StemRef[]> {
		const known = this.stemUrls.get(stemId);
		if (known) return known;
		const fixture = SONGS.find((song) => song.id === stemId);
		if (!fixture) return [];
		return this.materializeStems(stemId, fixture);
	}

	async listSongs() {
		return this.songs;
	}

	async song(id: string) {
		return this.songs.find((song) => song.id === id) ?? null;
	}

	async importSong(file: File) {
		const fixture = SONGS[0];
		const song: Song = {
			id: `import-${hex(6)}`,
			title: file.name.replace(/\.[^.]+$/, ""),
			artist: "Imported",
			duration_sec: fixture?.durationSec ?? 0,
			status: "processing",
			stem_id: null,
			notes: null,
		};
		this.songs.unshift(song);

		const separate = this.queue<SeparateResult>(() => {
			this.materializeStems(song.id, fixture);
			song.stem_id = song.id;
			return { stem_id: song.id, stems: ["instrumental.wav", "vocals.wav"] };
		}, 2200);
		const transcribe = this.queue<Note[]>(() => {
			song.notes = fixture?.notes ?? [];
			song.status = "ready";
			return song.notes;
		}, 3800);

		return { song, separateJobId: separate.id, transcribeJobId: transcribe.id };
	}

	async analyzeTake(_songId: string, partStart: number, partEnd: number) {
		return this.queue<FeedbackResult>(
			() => ({
				part_start: partStart,
				part_end: partEnd,
				take_url: "",
				headline: "You know the tune. Work on when you come in.",
				score: 78,
				metrics: METRICS,
			}),
			2400,
		);
	}

	private materializeStems(stemId: string, fixture: SongFixture | undefined) {
		if (!fixture) return [];
		const refs: StemRef[] = [
			{
				name: "instrumental.wav",
				url: toObjectUrl(renderInstrumental(fixture), SYNTH_SAMPLE_RATE),
			},
			{
				name: "vocals.wav",
				url: toObjectUrl(renderReferenceVocal(fixture), SYNTH_SAMPLE_RATE),
			},
		];
		this.stemUrls.set(stemId, refs);
		return refs;
	}
}
