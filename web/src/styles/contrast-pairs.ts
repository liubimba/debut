export type ContrastPair = {
	fg: string;
	bg: string;
	min: number;
	why: string;
};

const SURFACES = ["bg", "surface", "surface-2"];

const textOnEverySurface = (fg: string, why: string): ContrastPair[] =>
	SURFACES.map((bg) => ({ fg, bg, min: 4.5, why }));

const fillOnEverySurface = (fg: string, why: string): ContrastPair[] =>
	SURFACES.map((bg) => ({ fg, bg, min: 3, why }));

export const CONTRAST_PAIRS: ContrastPair[] = [
	...textOnEverySurface("text", "primary copy"),
	...textOnEverySurface("text-muted", "secondary copy and labels"),
	...textOnEverySurface(
		"accent-text",
		"accent used as text, never the fill value",
	),
	...textOnEverySurface("error-text", "red as text needs its own token"),

	...fillOnEverySurface("accent", "buttons, the live pitch line, progress"),
	...fillOnEverySurface("error", "off-note state"),

	{
		fg: "note",
		bg: "bg",
		min: 3,
		why: "target notes must read on the roll background",
	},
	{ fg: "note-active", bg: "bg", min: 3, why: "the note you are on right now" },

	{
		fg: "on-accent",
		bg: "accent",
		min: 4.5,
		why: "label on the primary button",
	},
	{ fg: "on-error", bg: "error", min: 4.5, why: "label on an error fill" },
];
