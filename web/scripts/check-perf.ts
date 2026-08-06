import { chromium } from "playwright";

const BASE = process.argv[2] ?? "http://localhost:4173";
const ROUTE = process.argv[3] ?? "/styleguide";
const CPU_THROTTLE = 4;
const SAMPLE_MS = 6000;

const BUDGET = {
	p95FrameMs: 20,
	longTaskMs: 200,
	jsKilobytes: 400,
};

const run = async () => {
	const browser = await chromium.launch();
	const context = await browser.newContext({
		viewport: { width: 390, height: 844 },
	});
	const page = await context.newPage();

	let jsBytes = 0;
	page.on("response", async (response) => {
		if (!/javascript/.test(response.headers()["content-type"] ?? "")) return;
		try {
			jsBytes += (await response.body()).byteLength;
		} catch {
			return;
		}
	});

	const cdp = await context.newCDPSession(page);
	await cdp.send("Emulation.setCPUThrottlingRate", { rate: CPU_THROTTLE });

	await page.goto(`${BASE}${ROUTE}`, { waitUntil: "networkidle" });

	const result = await page.evaluate(async (sampleMs) => {
		const frames: number[] = [];
		const longTasks: number[] = [];
		const observer = new PerformanceObserver((list) => {
			for (const entry of list.getEntries()) longTasks.push(entry.duration);
		});
		observer.observe({ entryTypes: ["longtask"] as unknown as string[] });

		let previous = performance.now();
		const started = previous;
		while (previous - started < sampleMs) {
			const now = await new Promise<number>((resolve) =>
				requestAnimationFrame(resolve),
			);
			frames.push(now - previous);
			previous = now;
			window.scrollBy(0, 12);
		}
		observer.disconnect();
		return { frames: frames.slice(1), longTasks };
	}, SAMPLE_MS);

	await browser.close();

	const sorted = [...result.frames].sort((a, b) => a - b);
	const p95 = sorted[Math.floor(sorted.length * 0.95)] ?? 0;
	const worstTask = Math.max(0, ...result.longTasks);
	const jsKb = jsBytes / 1024;

	const rows = [
		["p95 frame", `${p95.toFixed(1)} ms`, p95 <= BUDGET.p95FrameMs],
		[
			"worst long task",
			`${worstTask.toFixed(0)} ms`,
			worstTask <= BUDGET.longTaskMs,
		],
		["js transferred", `${jsKb.toFixed(0)} KB`, jsKb <= BUDGET.jsKilobytes],
	] as const;

	console.log(
		`${ROUTE} at ${CPU_THROTTLE}x CPU throttle, ${result.frames.length} frames`,
	);
	for (const [name, value, ok] of rows) {
		console.log(`  ${ok ? "ok  " : "FAIL"} ${name.padEnd(18)} ${value}`);
	}

	if (rows.some(([, , ok]) => !ok)) process.exit(1);
};

run();
