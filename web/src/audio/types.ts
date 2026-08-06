export type PitchFrame = {
	audioTime: number;
	freqHz: number;
	confidence: number;
};

export type Pitch = {
	midi: number;
	freqHz: number;
	centsOffset: number;
	confidence: number;
};

export type Note = {
	pitch: {
		midi: number;
		freq_hz: number;
		cents_offset: number;
		confidence: number;
	};
	start_time: number;
	end_time: number;
};

export type SingAlongPhase =
	| "idle"
	| "calibrating"
	| "singing"
	| "paused_out_of_note"
	| "done";
