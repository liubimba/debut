import type { DebutApi } from "./DebutApi";
import type {
	FeedbackResult,
	Job,
	Note,
	SeparateResult,
	Song,
	StemRef,
} from "./types";

const NOT_ON_BACKEND = (name: string) =>
	new Error(
		`${name} has no backend endpoint yet — run in mock mode (VITE_API_MODE=mock) or add it to the FastAPI service`,
	);

export class HttpDebutApi implements DebutApi {
	constructor(private readonly baseUrl: string) {}

	private async request<Result>(
		path: string,
		init?: RequestInit,
	): Promise<Result> {
		const response = await fetch(`${this.baseUrl}${path}`, init);
		if (!response.ok) {
			const detail = await response.text();
			throw new Error(`${response.status} ${path}: ${detail.slice(0, 200)}`);
		}
		return (await response.json()) as Result;
	}

	async transcribe(file: File) {
		const body = new FormData();
		body.append("file", file);
		return this.request<Job<Note[]>>("/audio/transcribe", {
			method: "POST",
			body,
		});
	}

	async separate(file: File, sampleRate = 44100, stemId?: string) {
		const body = new FormData();
		body.append("file", file);
		body.append("sample_rate", String(sampleRate));
		if (stemId) body.append("stem_id", stemId);
		return this.request<Job<SeparateResult>>("/audio/separate", {
			method: "POST",
			body,
		});
	}

	async job<Result>(id: string) {
		return this.request<Job<Result>>(`/jobs/${encodeURIComponent(id)}`);
	}

	async stems(stemId: string) {
		return this.request<StemRef[]>(`/stems/${encodeURIComponent(stemId)}`);
	}

	async listSongs(): Promise<Song[]> {
		throw NOT_ON_BACKEND("listSongs");
	}

	async song(): Promise<Song | null> {
		throw NOT_ON_BACKEND("song");
	}

	async importSong(): Promise<{
		song: Song;
		separateJobId: string;
		transcribeJobId: string;
	}> {
		throw NOT_ON_BACKEND("importSong");
	}

	async analyzeTake(): Promise<Job<FeedbackResult>> {
		throw NOT_ON_BACKEND("analyzeTake");
	}
}
