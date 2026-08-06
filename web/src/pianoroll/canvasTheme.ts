const TOKENS = [
	"bg",
	"surface",
	"surface-2",
	"border",
	"text-muted",
	"accent",
	"error",
	"note",
	"note-active",
] as const;

export type CanvasToken = (typeof TOKENS)[number];
export type CanvasPalette = Record<CanvasToken, string>;

const read = (): CanvasPalette => {
	const style = getComputedStyle(document.documentElement);
	const palette = {} as CanvasPalette;
	for (const token of TOKENS)
		palette[token] = style.getPropertyValue(`--${token}`).trim();
	return palette;
};

export const watchPalette = (onChange: (palette: CanvasPalette) => void) => {
	onChange(read());
	const observer = new MutationObserver(() => onChange(read()));
	observer.observe(document.documentElement, {
		attributes: true,
		attributeFilter: ["class"],
	});
	return () => observer.disconnect();
};
