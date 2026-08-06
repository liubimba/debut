import type {
	FeedbackResult,
	Job,
	Note,
	SeparateResult,
	Song,
	StemRef,
} from "./types";

export interface DebutApi {
	transcribe(file: File): Promise<Job<Note[]>>;
	separate(
		file: File,
		sampleRate?: number,
		stemId?: string,
	): Promise<Job<SeparateResult>>;
	job<Result>(id: string): Promise<Job<Result>>;
	stems(stemId: string): Promise<StemRef[]>;

	listSongs(): Promise<Song[]>;
	song(id: string): Promise<Song | null>;
	importSong(
		file: File,
	): Promise<{ song: Song; separateJobId: string; transcribeJobId: string }>;
	analyzeTake(
		songId: string,
		partStart: number,
		partEnd: number,
	): Promise<Job<FeedbackResult>>;
}

export const INVENTED_BY_MOCK = [
	"listSongs",
	"song",
	"importSong",
	"analyzeTake",
] as const;
