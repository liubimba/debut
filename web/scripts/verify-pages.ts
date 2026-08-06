import { mkdir } from "node:fs/promises";
import { chromium } from "playwright";
import { ROUTES, THEMES, VIEWPORTS } from "./routes";

const BASE = process.argv[2] ?? "http://localhost:4173";
const SHOTS = "screenshots";

type Problem = {
	where: string;
	what: string;
};

const run = async () => {
	await mkdir(SHOTS, { recursive: true });
	const browser = await chromium.launch();
	const problems: Problem[] = [];

	for (const theme of THEMES) {
		for (const viewport of VIEWPORTS) {
			const context = await browser.newContext({
				viewport: { width: viewport.width, height: viewport.height },
				deviceScaleFactor: 2,
			});
			const page = await context.newPage();
			await page.goto(BASE);
			await page.evaluate(
				(value) => localStorage.setItem("debut-theme", value),
				theme,
			);

			for (const route of ROUTES) {
				const where = `${route} ${theme} ${viewport.name}`;
				page.removeAllListeners();
				page.on("console", (message) => {
					if (message.type() === "error")
						problems.push({ where, what: `console: ${message.text()}` });
				});
				page.on("pageerror", (error) => {
					problems.push({ where, what: `pageerror: ${error.message}` });
				});
				page.on("requestfailed", (request) => {
					problems.push({
						where,
						what: `request failed: ${request.url()} ${request.failure()?.errorText ?? ""}`,
					});
				});

				await page.goto(`${BASE}${route}`, { waitUntil: "networkidle" });

				const overflow = await page.evaluate(
					() =>
						document.documentElement.scrollWidth -
						document.documentElement.clientWidth,
				);
				if (overflow > 0) {
					problems.push({ where, what: `horizontal overflow: ${overflow}px` });
				}

				const emptyVars = await page.evaluate(() => {
					const bad: string[] = [];
					for (const element of document.querySelectorAll<HTMLElement>(
						"[style]",
					)) {
						const inline = element.getAttribute("style") ?? "";
						if (/:\s*(;|$)/.test(inline)) bad.push(inline.slice(0, 60));
					}
					return bad;
				});
				for (const inline of emptyVars) {
					problems.push({ where, what: `empty inline style value: ${inline}` });
				}

				const rendered = await page.evaluate(
					() =>
						(document.getElementById("root")?.textContent ?? "").trim().length,
				);
				if (rendered === 0)
					problems.push({ where, what: "root rendered empty" });

				const slug =
					route.replace(/\W+/g, "-").replace(/^-|-$/g, "") || "index";
				await page.screenshot({
					path: `${SHOTS}/${slug}-${theme}-${viewport.name}.png`,
					fullPage: true,
				});
			}
			await context.close();
		}
	}

	await browser.close();

	if (problems.length > 0) {
		for (const { where, what } of problems)
			console.error(`FAIL ${where.padEnd(34)} ${what}`);
		console.error(`\n${problems.length} problems`);
		process.exit(1);
	}
	console.log(
		`ok — ${ROUTES.length} routes x ${THEMES.length} themes x ${VIEWPORTS.length} widths, screenshots in ${SHOTS}/`,
	);
};

run();
