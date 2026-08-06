import { createYinDetector } from "../dsp/yin";

type Options = {
	windowSize: number;
	hopSize: number;
	fminHz: number;
	fmaxHz: number;
	threshold: number;
};

class PitchProcessor extends AudioWorkletProcessor {
	private readonly ring: Float32Array;
	private readonly window: Float32Array;
	private readonly windowSize: number;
	private readonly hopSize: number;
	private readonly detect: (buffer: Float32Array) => {
		freqHz: number;
		confidence: number;
	};
	private write = 0;
	private sinceHop = 0;
	private running = true;

	constructor(options: AudioWorkletNodeOptions) {
		super();
		const settings = options.processorOptions as Options;
		this.windowSize = settings.windowSize;
		this.hopSize = settings.hopSize;
		this.ring = new Float32Array(settings.windowSize);
		this.window = new Float32Array(settings.windowSize);
		this.detect = createYinDetector({
			sampleRate,
			windowSize: settings.windowSize,
			fminHz: settings.fminHz,
			fmaxHz: settings.fmaxHz,
			threshold: settings.threshold,
		});
		this.port.onmessage = (event) => {
			if (event.data === "stop") this.running = false;
		};
	}

	process(inputs: Float32Array[][]) {
		const channel = inputs[0]?.[0];
		if (!channel) return this.running;

		for (let i = 0; i < channel.length; i += 1) {
			this.ring[this.write] = channel[i] ?? 0;
			this.write = (this.write + 1) % this.windowSize;
		}
		this.sinceHop += channel.length;

		if (this.sinceHop < this.hopSize) return this.running;
		this.sinceHop = 0;

		for (let i = 0; i < this.windowSize; i += 1) {
			this.window[i] = this.ring[(this.write + i) % this.windowSize] ?? 0;
		}

		const { freqHz, confidence } = this.detect(this.window);
		this.port.postMessage({ audioTime: currentTime, freqHz, confidence });
		return this.running;
	}
}

registerProcessor("pitch-processor", PitchProcessor);
