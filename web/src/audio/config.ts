export type EngineConfig = {
	windowSize: number;
	hopSize: number;
	fminHz: number;
	fmaxHz: number;
	yinThreshold: number;
	minConfidence: number;
	enterCents: number;
	exitCents: number;
	enterHoldMs: number;
	exitHoldMs: number;
	unvoicedGraceMs: number;
	latencyCompensationMs: number;
	fadeMs: number;
};

export const DEFAULT_CONFIG: EngineConfig = {
	windowSize: 2048,
	hopSize: 512,
	fminHz: 65,
	fmaxHz: 1200,
	yinThreshold: 0.15,
	minConfidence: 0.5,
	enterCents: 50,
	exitCents: 80,
	enterHoldMs: 60,
	exitHoldMs: 180,
	unvoicedGraceMs: 250,
	latencyCompensationMs: 0,
	fadeMs: 8,
};

export const LIMITS = {
	enterCents: [10, 100],
	exitCents: [20, 200],
	enterHoldMs: [0, 300],
	exitHoldMs: [0, 800],
	unvoicedGraceMs: [0, 1000],
	latencyCompensationMs: [-200, 400],
	minConfidence: [0.1, 0.95],
} as const satisfies Record<string, readonly [number, number]>;

export const clampConfig = (config: EngineConfig): EngineConfig => {
	const next = { ...config };
	for (const [key, [min, max]] of Object.entries(LIMITS)) {
		const field = key as keyof typeof LIMITS;
		next[field] = Math.min(max, Math.max(min, config[field]));
	}
	if (next.exitCents <= next.enterCents) next.exitCents = next.enterCents + 10;
	return next;
};
