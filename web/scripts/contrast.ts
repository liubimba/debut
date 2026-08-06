export type Rgb = [number, number, number];

export const parseColor = (value: string): Rgb => {
	const match = value.match(/-?[\d.]+/g);
	if (!match || match.length < 3)
		throw new Error(`cannot parse color: ${value}`);
	if (value.startsWith("oklch")) {
		const [l, c, h] = match.map(Number) as [number, number, number];
		return oklchToRgb(l, c, h);
	}
	return [Number(match[0]), Number(match[1]), Number(match[2])];
};

const srgbToLinear = (channel: number) => {
	const c = channel / 255;
	return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
};

const linearToSrgb = (c: number) =>
	255 * (c <= 0.0031308 ? c * 12.92 : 1.055 * c ** (1 / 2.4) - 0.055);

const oklchToRgb = (L: number, C: number, H: number): Rgb => {
	const rad = (H * Math.PI) / 180;
	const a = C * Math.cos(rad);
	const b = C * Math.sin(rad);
	const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
	const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
	const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
	return [
		linearToSrgb(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
		linearToSrgb(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
		linearToSrgb(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s),
	];
};

export const relativeLuminance = ([r, g, b]: Rgb) =>
	0.2126 * srgbToLinear(r) +
	0.7152 * srgbToLinear(g) +
	0.0722 * srgbToLinear(b);

export const contrastRatio = (fg: Rgb, bg: Rgb) => {
	const a = relativeLuminance(fg);
	const b = relativeLuminance(bg);
	return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
};
