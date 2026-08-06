import type { PitchFrame } from "./types";

export class FrameBus {
	private readonly times: Float64Array;
	private readonly freqs: Float32Array;
	private readonly confidences: Float32Array;
	private cursor = 0;
	private filled = 0;

	latest: PitchFrame = { audioTime: 0, freqHz: 0, confidence: 0 };

	constructor(readonly capacity: number) {
		this.times = new Float64Array(capacity);
		this.freqs = new Float32Array(capacity);
		this.confidences = new Float32Array(capacity);
	}

	push(frame: PitchFrame) {
		this.times[this.cursor] = frame.audioTime;
		this.freqs[this.cursor] = frame.freqHz;
		this.confidences[this.cursor] = frame.confidence;
		this.cursor = (this.cursor + 1) % this.capacity;
		this.filled = Math.min(this.filled + 1, this.capacity);
		this.latest = frame;
	}

	get size() {
		return this.filled;
	}

	at(index: number): PitchFrame {
		const slot =
			(this.cursor - this.filled + index + this.capacity * 2) % this.capacity;
		return {
			audioTime: this.times[slot] ?? 0,
			freqHz: this.freqs[slot] ?? 0,
			confidence: this.confidences[slot] ?? 0,
		};
	}

	forEachSince(
		since: number,
		visit: (audioTime: number, freqHz: number, confidence: number) => void,
	) {
		for (let index = 0; index < this.filled; index += 1) {
			const slot =
				(this.cursor - this.filled + index + this.capacity * 2) % this.capacity;
			const time = this.times[slot] ?? 0;
			if (time < since) continue;
			visit(time, this.freqs[slot] ?? 0, this.confidences[slot] ?? 0);
		}
	}

	clear() {
		this.cursor = 0;
		this.filled = 0;
		this.latest = { audioTime: 0, freqHz: 0, confidence: 0 };
	}
}
