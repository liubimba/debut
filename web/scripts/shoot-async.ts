import { chromium } from "playwright";
import { THEMES, VIEWPORTS } from "./routes";

const BASE = process.argv[2] ?? "http://localhost:4173";

const TARGETS = [
	{
		route: "/song/aurora-lines/feedback",
		slug: "feedback-ready",
		waitMs: 5000,
	},
	{
		route: "/song/aurora-lines",
		slug: "song-more-options",
		waitMs: 400,
		open: "More options",
	},
	{
		route: "/settings",
		slug: "settings-advanced",
		waitMs: 400,
		open: "Advanced",
	},
];

const run = async () => {
	const browser = await chromium.launch();
	for (const theme of THEMES) {
		for (const view of VIEWPORTS) {
			const context = await browser.newContext({
				viewport: { width: view.width, height: view.height },
				deviceScaleFactor: 2,
			});
			const page = await context.newPage();
			await page.goto(BASE);
			await page.evaluate(
				(value) => localStorage.setItem("debut-theme", value),
				theme,
			);

			for (const target of TARGETS) {
				await page.goto(`${BASE}${target.route}`, { waitUntil: "networkidle" });
				if (target.open) {
					await page.getByRole("button", { name: target.open }).click();
				}
				await page.waitForTimeout(target.waitMs);
				await page.screenshot({
					path: `screenshots/${target.slug}-${theme}-${view.name}.png`,
					fullPage: true,
				});
			}
			await context.close();
		}
	}
	await browser.close();
	console.log("done");
};

run();
