export const encodeWav = (samples: Float32Array, sampleRate: number) => {
	const bytes = new ArrayBuffer(44 + samples.length * 2);
	const view = new DataView(bytes);

	const writeAscii = (offset: number, text: string) => {
		for (let i = 0; i < text.length; i += 1)
			view.setUint8(offset + i, text.charCodeAt(i));
	};

	writeAscii(0, "RIFF");
	view.setUint32(4, 36 + samples.length * 2, true);
	writeAscii(8, "WAVE");
	writeAscii(12, "fmt ");
	view.setUint32(16, 16, true);
	view.setUint16(20, 1, true);
	view.setUint16(22, 1, true);
	view.setUint32(24, sampleRate, true);
	view.setUint32(28, sampleRate * 2, true);
	view.setUint16(32, 2, true);
	view.setUint16(34, 16, true);
	writeAscii(36, "data");
	view.setUint32(40, samples.length * 2, true);

	let peak = 0;
	for (const sample of samples) peak = Math.max(peak, Math.abs(sample));
	const gain = peak > 0.99 ? 0.99 / peak : 1;

	for (let i = 0; i < samples.length; i += 1) {
		const clamped = Math.max(-1, Math.min(1, (samples[i] ?? 0) * gain));
		view.setInt16(44 + i * 2, Math.round(clamped * 32767), true);
	}

	return new Blob([bytes], { type: "audio/wav" });
};

export const toObjectUrl = (samples: Float32Array, sampleRate: number) =>
	URL.createObjectURL(encodeWav(samples, sampleRate));
