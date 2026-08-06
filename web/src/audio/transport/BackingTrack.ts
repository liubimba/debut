export type TrackId = string;

type Voice = {
	source: AudioBufferSourceNode;
	gain: GainNode;
};

export class BackingTrack {
	private readonly buffers = new Map<TrackId, AudioBuffer>();
	private readonly gains = new Map<TrackId, GainNode>();
	private readonly muted = new Set<TrackId>();
	private voices: Voice[] = [];
	private startedAtCtxTime = 0;
	private offsetSec = 0;
	private playing = false;

	constructor(
		private readonly context: AudioContext,
		private readonly fadeMs: number,
	) {}

	async load(id: TrackId, url: string) {
		const response = await fetch(url);
		const bytes = await response.arrayBuffer();
		this.buffers.set(id, await this.context.decodeAudioData(bytes));
		const gain = this.context.createGain();
		gain.gain.value = this.muted.has(id) ? 0 : 1;
		gain.connect(this.context.destination);
		this.gains.set(id, gain);
	}

	get durationSec() {
		let longest = 0;
		for (const buffer of this.buffers.values())
			longest = Math.max(longest, buffer.duration);
		return longest;
	}

	get isPlaying() {
		return this.playing;
	}

	positionSec() {
		if (!this.playing) return this.offsetSec;
		return this.offsetSec + (this.context.currentTime - this.startedAtCtxTime);
	}

	setMuted(id: TrackId, muted: boolean) {
		if (muted) this.muted.add(id);
		else this.muted.delete(id);
		const gain = this.gains.get(id);
		if (!gain) return;
		const fade = this.fadeMs / 1000;
		gain.gain.cancelScheduledValues(this.context.currentTime);
		gain.gain.setTargetAtTime(
			muted ? 0 : 1,
			this.context.currentTime,
			fade / 3,
		);
	}

	isMuted(id: TrackId) {
		return this.muted.has(id);
	}

	play() {
		if (this.playing || this.buffers.size === 0) return;
		const at = this.context.currentTime;
		const fade = this.fadeMs / 1000;

		this.voices = [];
		for (const [id, buffer] of this.buffers) {
			const gain = this.gains.get(id);
			if (!gain) continue;
			const source = this.context.createBufferSource();
			source.buffer = buffer;
			const voiceGain = this.context.createGain();
			voiceGain.gain.setValueAtTime(0, at);
			voiceGain.gain.linearRampToValueAtTime(1, at + fade);
			source.connect(voiceGain);
			voiceGain.connect(gain);
			source.start(at, Math.min(this.offsetSec, buffer.duration));
			this.voices.push({ source, gain: voiceGain });
		}

		this.startedAtCtxTime = at;
		this.playing = true;
	}

	pause() {
		if (!this.playing) return;
		this.offsetSec = this.positionSec();
		this.playing = false;
		this.stopVoices();
	}

	seek(seconds: number) {
		const wasPlaying = this.playing;
		if (wasPlaying) this.pause();
		this.offsetSec = Math.max(0, seconds);
		if (wasPlaying) this.play();
	}

	dispose() {
		this.stopVoices();
		for (const gain of this.gains.values()) gain.disconnect();
		this.gains.clear();
		this.buffers.clear();
	}

	private stopVoices() {
		const at = this.context.currentTime;
		const fade = this.fadeMs / 1000;
		for (const voice of this.voices) {
			voice.gain.gain.cancelScheduledValues(at);
			voice.gain.gain.setValueAtTime(voice.gain.gain.value, at);
			voice.gain.gain.linearRampToValueAtTime(0, at + fade);
			voice.source.stop(at + fade + 0.01);
		}
		this.voices = [];
	}
}
