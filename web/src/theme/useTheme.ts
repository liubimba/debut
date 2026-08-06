import { useCallback, useEffect, useState } from "react";

export type ThemeMode = "dark" | "light" | "system";
export type ResolvedTheme = "dark" | "light";

const STORAGE_KEY = "debut-theme";

const readStoredMode = (): ThemeMode => {
	const stored = localStorage.getItem(STORAGE_KEY);
	return stored === "dark" || stored === "light" ? stored : "system";
};

const readAppliedTheme = (): ResolvedTheme =>
	document.documentElement.classList.contains("theme-dark") ? "dark" : "light";

const systemTheme = (): ResolvedTheme =>
	matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";

const apply = (theme: ResolvedTheme) => {
	document.documentElement.className = `theme-${theme}`;
	document.documentElement.style.backgroundColor = "";
};

export const useTheme = () => {
	const [mode, setMode] = useState<ThemeMode>(readStoredMode);
	const [resolved, setResolved] = useState<ResolvedTheme>(readAppliedTheme);

	useEffect(() => {
		if (mode !== "system") return;
		const media = matchMedia("(prefers-color-scheme: dark)");
		const sync = () => {
			const next = systemTheme();
			apply(next);
			setResolved(next);
		};
		media.addEventListener("change", sync);
		return () => media.removeEventListener("change", sync);
	}, [mode]);

	const choose = useCallback((next: ThemeMode) => {
		if (next === "system") localStorage.removeItem(STORAGE_KEY);
		else localStorage.setItem(STORAGE_KEY, next);
		const theme = next === "system" ? systemTheme() : next;
		apply(theme);
		setMode(next);
		setResolved(theme);
	}, []);

	const toggle = useCallback(() => {
		choose(readAppliedTheme() === "dark" ? "light" : "dark");
	}, [choose]);

	return { mode, resolved, choose, toggle };
};
