import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { chromium, type Page } from "playwright";
import { FAKE_MIC_ARGS, writeVoiceWav } from "./fakeVoice";

const BASE = process.argv[2] ?? "http://localhost:4173";
const ON_NOTE = 440;
const OFF_NOTE = 440 * 2 ** (400 / 1200);

const readPhase = async (page: Page) =>
	((await page.getByTestId("phase").textContent()) ?? "").trim();

const run = async () => {
	const dir = await mkdtemp(join(tmpdir(), "debut-sing-"));
	const wav = join(dir, "voice.wav");
	await writeVoiceWav(wav, [
		{ hz: ON_NOTE, seconds: 8 },
		{ hz: OFF_NOTE, seconds: 4 },
		{ hz: ON_NOTE, seconds: 8 },
	]);

	const browser = await chromium.launch({ args: FAKE_MIC_ARGS(wav) });
	const context = await browser.newContext({ permissions: ["microphone"] });
	const page = await context.newPage();

	const failures: string[] = [];
	page.on("pageerror", (error) => failures.push(`pageerror: ${error.message}`));
	page.on("console", (message) => {
		if (message.type() === "error") failures.push(`console: ${message.text()}`);
	});

	await page.goto(`${BASE}/song/sustained-a/singalong`, {
		waitUntil: "networkidle",
	});

	const startButton = page.getByTestId("start");
	if (await startButton.isEnabled()) {
		failures.push("start was enabled before the two setup steps were done");
	}

	await page.getByRole("button", { name: "They are on" }).click();
	await page.getByRole("button", { name: "Turn on the microphone" }).click();
	await startButton.click({ timeout: 20000 });

	const phases: string[] = [];
	for (let i = 0; i < 120; i += 1) {
		const phase = await readPhase(page).catch(() => "");
		if (phase && phases.at(-1) !== phase) phases.push(phase);
		await page.waitForTimeout(150);
	}

	await browser.close();

	console.log(`phase transitions: ${phases.join(" -> ")}`);

	if (failures.length > 0) {
		for (const failure of failures) console.error(`FAIL ${failure}`);
		process.exit(1);
	}
	if (!phases.includes("singing")) {
		console.error(
			"FAIL never started singing — mic, worklet or track is broken",
		);
		process.exit(1);
	}
	if (!phases.includes("paused_out_of_note")) {
		console.error("FAIL never stopped the music on a sustained wrong note");
		process.exit(1);
	}
	if (phases.lastIndexOf("singing") <= phases.indexOf("paused_out_of_note")) {
		console.error("FAIL never resumed once the singer came back to the note");
		process.exit(1);
	}
	console.log(
		"\nok — mic check gates the start, music stops on a sustained miss and resumes on return",
	);
};

run();
