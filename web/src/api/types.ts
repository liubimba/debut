export type JobState = "QUEUED" | "RUNNING" | "FINISHED" | "FAILED";

export type Job<Result = unknown> = {
	id: string;
	state: JobState;
	started_at: string;
	finished_at: string | null;
	error_message: string | null;
	result: Result | null;
};

export type Pitch = {
	midi: number;
	freq_hz: number;
	cents_offset: number;
	confidence: number;
};

export type Note = {
	pitch: Pitch;
	start_time: number;
	end_time: number;
};

export type SeparateResult = {
	stem_id: string;
	stems: string[];
};

export type StemRef = {
	name: string;
	url: string;
};

export type MetricVerdict = "good" | "work-on" | "neutral";

export type FeedbackMetric = {
	key: string;
	label: string;
	value: string;
	fill: number;
	verdict: MetricVerdict;
	plain: string;
	advice: string | null;
};

export type FeedbackResult = {
	part_start: number;
	part_end: number;
	take_url: string;
	headline: string;
	score: number;
	metrics: FeedbackMetric[];
};

export type Song = {
	id: string;
	title: string;
	artist: string;
	duration_sec: number;
	status: "processing" | "ready" | "failed";
	stem_id: string | null;
	notes: Note[] | null;
};
