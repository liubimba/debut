export class TraceRing {
	private readonly times: Float64Array;
	private readonly midis: Float32Array;
	private readonly flags: Uint8Array;
	private cursor = 0;
	private filled = 0;

	constructor(readonly capacity: number) {
		this.times = new Float64Array(capacity);
		this.midis = new Float32Array(capacity);
		this.flags = new Uint8Array(capacity);
	}

	push(time: number, midiFloat: number, voiced: boolean, onNote: boolean) {
		this.times[this.cursor] = time;
		this.midis[this.cursor] = midiFloat;
		this.flags[this.cursor] = (voiced ? 1 : 0) | (onNote ? 2 : 0);
		this.cursor = (this.cursor + 1) % this.capacity;
		this.filled = Math.min(this.filled + 1, this.capacity);
	}

	get size() {
		return this.filled;
	}

	forEach(
		visit: (
			time: number,
			midiFloat: number,
			voiced: boolean,
			onNote: boolean,
		) => void,
	) {
		for (let index = 0; index < this.filled; index += 1) {
			const slot =
				(this.cursor - this.filled + index + this.capacity * 2) % this.capacity;
			const flag = this.flags[slot] ?? 0;
			visit(
				this.times[slot] ?? 0,
				this.midis[slot] ?? 0,
				(flag & 1) === 1,
				(flag & 2) === 2,
			);
		}
	}

	clear() {
		this.cursor = 0;
		this.filled = 0;
	}
}
