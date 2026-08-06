import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { chromium } from "playwright";
import { FAKE_MIC_ARGS, writeVoiceWav } from "./fakeVoice";
import { THEMES, VIEWPORTS } from "./routes";

const BASE = process.argv[2] ?? "http://localhost:4173";

const run = async () => {
	const dir = await mkdtemp(join(tmpdir(), "debut-live-"));
	const wav = join(dir, "voice.wav");
	await writeVoiceWav(wav, [
		{ hz: 440 * 2 ** (16 / 1200), seconds: 14 },
		{ hz: 440 * 2 ** (320 / 1200), seconds: 10 },
	]);

	const browser = await chromium.launch({ args: FAKE_MIC_ARGS(wav) });

	for (const theme of THEMES) {
		for (const view of VIEWPORTS) {
			const context = await browser.newContext({
				permissions: ["microphone"],
				viewport: { width: view.width, height: view.height },
				deviceScaleFactor: 2,
			});
			const page = await context.newPage();
			await page.goto(BASE);
			await page.evaluate(
				(value) => localStorage.setItem("debut-theme", value),
				theme,
			);
			await page.goto(`${BASE}/song/sustained-a/singalong`, {
				waitUntil: "networkidle",
			});
			await page.getByRole("button", { name: "They are on" }).click();
			await page
				.getByRole("button", { name: "Turn on the microphone" })
				.click();
			await page.waitForTimeout(1200);
			await page.screenshot({
				path: `screenshots/singalong-ready-${theme}-${view.name}.png`,
				fullPage: true,
			});

			await page.getByTestId("start").click({ timeout: 20000 });
			await page.waitForTimeout(6500);
			await page.screenshot({
				path: `screenshots/singalong-on-note-${theme}-${view.name}.png`,
			});

			await page.waitForTimeout(9000);
			await page.screenshot({
				path: `screenshots/singalong-stopped-${theme}-${view.name}.png`,
			});
			await context.close();
		}
	}

	await browser.close();
	console.log("done");
};

run();
