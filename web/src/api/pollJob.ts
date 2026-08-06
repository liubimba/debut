import type { DebutApi } from "./DebutApi";
import type { Job, JobState } from "./types";

export type PollOptions = {
	signal?: AbortSignal;
	onState?: (state: JobState, elapsedMs: number) => void;
	timeoutMs?: number;
};

const FIRST_DELAY_MS = 300;
const MAX_DELAY_MS = 1000;
const DEFAULT_TIMEOUT_MS = 180_000;

const wait = (ms: number, signal?: AbortSignal) =>
	new Promise<void>((resolve, reject) => {
		const timer = setTimeout(resolve, ms);
		signal?.addEventListener(
			"abort",
			() => {
				clearTimeout(timer);
				reject(new DOMException("Polling aborted", "AbortError"));
			},
			{ once: true },
		);
	});

export const pollJob = async <Result>(
	api: DebutApi,
	jobId: string,
	options: PollOptions = {},
): Promise<Job<Result>> => {
	const startedAt = Date.now();
	const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
	let delay = FIRST_DELAY_MS;
	let lastState: JobState | null = null;

	for (;;) {
		const job = await api.job<Result>(jobId);
		const elapsed = Date.now() - startedAt;

		if (job.state !== lastState) {
			lastState = job.state;
			options.onState?.(job.state, elapsed);
		}

		if (job.state === "FINISHED" || job.state === "FAILED") return job;
		if (elapsed > timeoutMs) {
			throw new Error(
				`Job ${jobId} still ${job.state} after ${Math.round(elapsed / 1000)}s`,
			);
		}

		await wait(delay, options.signal);
		delay = Math.min(MAX_DELAY_MS, Math.round(delay * 1.4));
	}
};
