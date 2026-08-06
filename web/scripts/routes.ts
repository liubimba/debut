export const ROUTES = [
	"/",
	"/?state=empty",
	"/?state=loading",
	"/?state=error",
	"/import",
	"/import?state=separating",
	"/import?state=error",
	"/import?state=done",
	"/song/aurora-lines",
	"/song/aurora-lines/singalong",
	"/song/aurora-lines/record",
	"/song/aurora-lines/feedback",
	"/settings",
	"/onboarding",
	"/styleguide",
];

export const VIEWPORTS = [
	{ name: "mobile", width: 390, height: 844 },
	{ name: "desktop", width: 1440, height: 900 },
	{ name: "wide", width: 2560, height: 1440 },
];

export const THEMES = ["light", "dark"] as const;
