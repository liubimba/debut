import { hzToMidiFloat } from "../audio/dsp/notes";
import type { FrameBus } from "../audio/FrameBus";
import type { Note } from "../audio/types";
import { type CanvasPalette, watchPalette } from "./canvasTheme";
import type { TraceRing } from "./TraceRing";
import {
	createViewport,
	midiToY,
	noteIsVisible,
	rowHeight,
	timeToX,
	type Viewport,
	visibleWindow,
} from "./viewport";

export type RollSources = {
	notes: () => Note[];
	positionSec: () => number;
	frames: () => FrameBus | null;
	trace: () => TraceRing;
	region: () => { start: number; end: number } | null;
	minConfidence: () => number;
	running: () => boolean;
	onNote: () => boolean;
};

export type RollOptions = {
	visibleSeconds: number;
	playheadFraction: number;
	maxDpr: number;
	scroll: "playhead" | "track";
	showPlayhead: boolean;
};

export const DEFAULT_ROLL_OPTIONS: RollOptions = {
	visibleSeconds: 4,
	playheadFraction: 0.3,
	maxDpr: 2,
	scroll: "playhead",
	showPlayhead: true,
};

export class PianoRollRenderer {
	private readonly ctx: CanvasRenderingContext2D;
	private readonly stopWatchingPalette: () => void;
	private readonly resizeObserver: ResizeObserver;
	private palette: CanvasPalette | null = null;
	private view: Viewport | null = null;
	private raf = 0;
	private disposed = false;
	private lastCapturedAudioTime = -1;

	constructor(
		private readonly canvas: HTMLCanvasElement,
		private readonly sources: RollSources,
		private readonly options: RollOptions = DEFAULT_ROLL_OPTIONS,
	) {
		const ctx = canvas.getContext("2d", { desynchronized: true, alpha: false });
		if (!ctx) throw new Error("2d canvas context unavailable");
		this.ctx = ctx;

		this.stopWatchingPalette = watchPalette((palette) => {
			this.palette = palette;
			this.requestFrame();
		});

		this.resizeObserver = new ResizeObserver(() => this.resize());
		this.resizeObserver.observe(canvas);
		this.resize();
		this.requestFrame();
	}

	dispose() {
		this.disposed = true;
		cancelAnimationFrame(this.raf);
		this.resizeObserver.disconnect();
		this.stopWatchingPalette();
	}

	requestFrame() {
		if (this.disposed || this.raf !== 0) return;
		this.raf = requestAnimationFrame(this.tick);
	}

	private resize() {
		const rect = this.canvas.getBoundingClientRect();
		if (rect.width === 0 || rect.height === 0) return;
		const dpr = Math.min(window.devicePixelRatio || 1, this.options.maxDpr);
		this.canvas.width = Math.round(rect.width * dpr);
		this.canvas.height = Math.round(rect.height * dpr);
		this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
		this.view = createViewport(
			rect.width,
			rect.height,
			this.sources.notes(),
			this.options.visibleSeconds,
			this.options.playheadFraction,
		);
		this.draw();
	}

	private readonly tick = () => {
		this.raf = 0;
		if (this.disposed) return;

		const live = this.sources.running();
		if (live) this.captureTrace();
		this.draw();

		if (live && !document.hidden) this.raf = requestAnimationFrame(this.tick);
	};

	private captureTrace() {
		const bus = this.sources.frames();
		if (!bus) return;
		const frame = bus.latest;
		if (frame.audioTime === this.lastCapturedAudioTime) return;
		this.lastCapturedAudioTime = frame.audioTime;
		const voiced =
			frame.freqHz > 0 && frame.confidence >= this.sources.minConfidence();
		this.sources
			.trace()
			.push(
				this.sources.positionSec(),
				voiced ? hzToMidiFloat(frame.freqHz) : 0,
				voiced,
				voiced && this.sources.onNote(),
			);
	}

	private draw() {
		const view = this.view;
		const palette = this.palette;
		if (!view || !palette) return;

		const ctx = this.ctx;
		const now = this.sources.positionSec();
		const origin = this.options.scroll === "track" ? 0 : now;
		const { from, to } = visibleWindow(view, origin);
		const rows = rowHeight(view);

		ctx.fillStyle = palette.bg;
		ctx.fillRect(0, 0, view.widthCss, view.heightCss);

		ctx.fillStyle = palette.surface;
		for (let midi = Math.ceil(view.midiLo); midi <= view.midiHi; midi += 1) {
			if (((midi % 12) + 12) % 12 !== 0) continue;
			ctx.fillRect(0, midiToY(view, midi) - rows / 2, view.widthCss, rows);
		}

		const region = this.sources.region();
		if (region) {
			const left = timeToX(view, region.start, origin);
			const right = timeToX(view, region.end, origin);
			ctx.fillStyle = palette.accent;
			ctx.globalAlpha = 0.08;
			ctx.fillRect(left, 0, right - left, view.heightCss);
			ctx.globalAlpha = 1;
		}

		const notes = this.sources.notes();
		const rest = new Path2D();
		const active = new Path2D();
		for (const note of notes) {
			if (!noteIsVisible(note, from, to)) continue;
			const x = timeToX(view, note.start_time, origin);
			const width = Math.max(
				3,
				(note.end_time - note.start_time) * view.pxPerSec,
			);
			const height = Math.max(6, rows * 0.62);
			const y = midiToY(view, note.pitch.midi) - height / 2;
			const isNow = note.start_time <= now && note.end_time >= now;
			(isNow ? active : rest).roundRect(x, y, width, height, height / 2);
		}

		ctx.lineWidth = 1.5;
		ctx.fillStyle = palette.note;
		ctx.strokeStyle = palette.note;
		ctx.globalAlpha = 0.14;
		ctx.fill(rest);
		ctx.globalAlpha = 0.9;
		ctx.stroke(rest);

		ctx.fillStyle = palette["note-active"];
		ctx.strokeStyle = palette["note-active"];
		ctx.globalAlpha = 0.16;
		ctx.fill(active);
		ctx.globalAlpha = 1;
		ctx.lineWidth = 2;
		ctx.stroke(active);

		this.strokeTrace(ctx, view, palette, origin, from, to, true);
		this.strokeTrace(ctx, view, palette, origin, from, to, false);

		if (this.options.showPlayhead) {
			ctx.fillStyle = palette.accent;
			ctx.fillRect(timeToX(view, now, origin) - 1, 0, 2, view.heightCss);
		}

		ctx.font = "500 11px system-ui, sans-serif";
		ctx.textBaseline = "middle";
		ctx.fillStyle = palette["text-muted"];
		for (let midi = Math.ceil(view.midiLo); midi <= view.midiHi; midi += 1) {
			if (((midi % 12) + 12) % 12 !== 0) continue;
			ctx.fillText(`C${Math.floor(midi / 12) - 1}`, 6, midiToY(view, midi));
		}
	}

	private strokeTrace(
		ctx: CanvasRenderingContext2D,
		view: Viewport,
		palette: CanvasPalette,
		origin: number,
		from: number,
		to: number,
		wantOnNote: boolean,
	) {
		ctx.lineWidth = 2.5;
		ctx.lineJoin = "round";
		ctx.lineCap = "round";
		ctx.strokeStyle = wantOnNote ? palette.accent : palette.error;
		ctx.beginPath();
		let pen = false;
		this.sources.trace().forEach((time, midiFloat, voiced, onNote) => {
			if (time < from || time > to || !voiced || onNote !== wantOnNote) {
				pen = false;
				return;
			}
			const x = timeToX(view, time, origin);
			const y = midiToY(view, midiFloat);
			if (pen) ctx.lineTo(x, y);
			else ctx.moveTo(x, y);
			pen = true;
		});
		ctx.stroke();
	}
}
