import type { EngineConfig } from "../config";
import type { PitchFrame } from "../types";
import workletUrl from "../worklet/pitch-processor.ts?worker&url";

export type MicHandle = {
	context: AudioContext;
	stop: () => Promise<void>;
};

export const openMicrophone = async (
	config: EngineConfig,
	onFrame: (frame: PitchFrame) => void,
): Promise<MicHandle> => {
	const stream = await navigator.mediaDevices.getUserMedia({
		audio: {
			echoCancellation: false,
			noiseSuppression: false,
			autoGainControl: false,
		},
	});

	const context = new AudioContext({ latencyHint: "interactive" });
	await context.audioWorklet.addModule(workletUrl);

	const source = context.createMediaStreamSource(stream);
	const node = new AudioWorkletNode(context, "pitch-processor", {
		numberOfInputs: 1,
		numberOfOutputs: 0,
		processorOptions: {
			windowSize: config.windowSize,
			hopSize: config.hopSize,
			fminHz: config.fminHz,
			fmaxHz: config.fmaxHz,
			threshold: config.yinThreshold,
		},
	});

	node.port.onmessage = (event) => onFrame(event.data as PitchFrame);
	source.connect(node);

	return {
		context,
		stop: async () => {
			node.port.postMessage("stop");
			node.port.onmessage = null;
			source.disconnect();
			node.disconnect();
			for (const track of stream.getTracks()) track.stop();
			await context.close();
		},
	};
};
