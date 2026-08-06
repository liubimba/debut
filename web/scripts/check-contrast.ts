import { chromium } from "playwright";
import { CONTRAST_PAIRS } from "../src/styles/contrast-pairs";
import { contrastRatio, parseColor } from "./contrast";

const BASE = process.argv[2] ?? "http://localhost:4173";
const THEMES = ["dark", "light"] as const;

const tokenNames = [
	...new Set(CONTRAST_PAIRS.flatMap((pair) => [pair.fg, pair.bg])),
];

const run = async () => {
	const browser = await chromium.launch();
	const page = await browser.newPage();
	let failures = 0;

	for (const theme of THEMES) {
		await page.goto(BASE);
		await page.evaluate(
			(value) => localStorage.setItem("debut-theme", value),
			theme,
		);
		await page.reload({ waitUntil: "networkidle" });

		const resolved = await page.evaluate((names) => {
			const style = getComputedStyle(document.documentElement);
			const probe = document.createElement("span");
			document.body.append(probe);
			const out: Record<string, string> = {};
			for (const name of names) {
				probe.style.color = "";
				probe.style.color = style.getPropertyValue(`--${name}`).trim();
				out[name] = getComputedStyle(probe).color;
			}
			probe.remove();
			return out;
		}, tokenNames);

		console.log(`\n${theme.toUpperCase()}`);
		for (const pair of CONTRAST_PAIRS) {
			const fg = resolved[pair.fg];
			const bg = resolved[pair.bg];
			if (!fg || !bg) {
				console.log(`  MISSING  --${pair.fg} on --${pair.bg}`);
				failures += 1;
				continue;
			}
			const ratio = contrastRatio(parseColor(fg), parseColor(bg));
			const ok = ratio >= pair.min;
			if (!ok) failures += 1;
			const line = `${pair.fg} on ${pair.bg}`.padEnd(34);
			console.log(
				`  ${ok ? "ok  " : "FAIL"} ${line} ${ratio.toFixed(2)} (min ${pair.min})${
					ok ? "" : ` — ${pair.why}`
				}`,
			);
		}
	}

	await browser.close();

	if (failures > 0) {
		console.error(`\n${failures} contrast failures`);
		process.exit(1);
	}
	console.log(`\nall ${CONTRAST_PAIRS.length * THEMES.length} pairs pass`);
};

run();
