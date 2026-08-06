export type YinOptions = {
	sampleRate: number;
	windowSize: number;
	fminHz: number;
	fmaxHz: number;
	threshold: number;
};

export type YinResult = {
	freqHz: number;
	confidence: number;
};

export const SILENT: YinResult = { freqHz: 0, confidence: 0 };

export const createYinDetector = ({
	sampleRate,
	windowSize,
	fminHz,
	fmaxHz,
	threshold,
}: YinOptions) => {
	const tauMin = Math.max(2, Math.floor(sampleRate / fmaxHz));
	const tauMax = Math.min(windowSize >> 1, Math.ceil(sampleRate / fminHz));
	const diff = new Float32Array(tauMax + 1);
	const normalized = new Float32Array(tauMax + 1);

	return (buffer: Float32Array): YinResult => {
		if (buffer.length < windowSize) return SILENT;

		let power = 0;
		for (let i = 0; i < windowSize; i += 1) {
			const sample = buffer[i] ?? 0;
			power += sample * sample;
		}
		if (Math.sqrt(power / windowSize) < 0.004) return SILENT;

		for (let tau = 1; tau <= tauMax; tau += 1) {
			let sum = 0;
			for (let i = 0; i + tau < windowSize; i += 1) {
				const delta = (buffer[i] ?? 0) - (buffer[i + tau] ?? 0);
				sum += delta * delta;
			}
			diff[tau] = sum;
		}

		normalized[0] = 1;
		let running = 0;
		for (let tau = 1; tau <= tauMax; tau += 1) {
			running += diff[tau] ?? 0;
			normalized[tau] = running === 0 ? 1 : ((diff[tau] ?? 0) * tau) / running;
		}

		let best = -1;
		for (let tau = tauMin; tau <= tauMax; tau += 1) {
			if ((normalized[tau] ?? 1) >= threshold) continue;
			while (
				tau + 1 <= tauMax &&
				(normalized[tau + 1] ?? 1) < (normalized[tau] ?? 1)
			) {
				tau += 1;
			}
			best = tau;
			break;
		}

		if (best < 0) {
			let lowest = 1;
			for (let tau = tauMin; tau <= tauMax; tau += 1) {
				if ((normalized[tau] ?? 1) < lowest) {
					lowest = normalized[tau] ?? 1;
					best = tau;
				}
			}
			if (best < 0 || lowest > 0.6) return SILENT;
		}

		const previous = normalized[best - 1] ?? normalized[best] ?? 1;
		const current = normalized[best] ?? 1;
		const next = normalized[best + 1] ?? current;
		const denominator = 2 * (2 * current - previous - next);
		const shift = denominator === 0 ? 0 : (next - previous) / denominator;
		const period = best + shift;

		if (period <= 0) return SILENT;

		return {
			freqHz: sampleRate / period,
			confidence: Math.max(0, Math.min(1, 1 - current)),
		};
	};
};
