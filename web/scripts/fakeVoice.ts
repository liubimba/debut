import { writeFile } from "node:fs/promises";

export const SAMPLE_RATE = 48000;

export type VoicePart = {
	hz: number;
	seconds: number;
};

export const writeVoiceWav = async (path: string, plan: VoicePart[]) => {
	const frames = plan.reduce(
		(sum, part) => sum + Math.round(part.seconds * SAMPLE_RATE),
		0,
	);
	const data = Buffer.alloc(frames * 2);
	let cursor = 0;
	let phase = 0;

	for (const part of plan) {
		const count = Math.round(part.seconds * SAMPLE_RATE);
		for (let i = 0; i < count; i += 1) {
			if (part.hz > 0) {
				phase += (2 * Math.PI * part.hz) / SAMPLE_RATE;
				const sample =
					0.5 * Math.sin(phase) +
					0.25 * Math.sin(2 * phase) +
					0.1 * Math.sin(3 * phase);
				data.writeInt16LE(
					Math.round(Math.max(-1, Math.min(1, sample * 0.7)) * 32767),
					cursor * 2,
				);
			}
			cursor += 1;
		}
	}

	const header = Buffer.alloc(44);
	header.write("RIFF", 0);
	header.writeUInt32LE(36 + data.length, 4);
	header.write("WAVE", 8);
	header.write("fmt ", 12);
	header.writeUInt32LE(16, 16);
	header.writeUInt16LE(1, 20);
	header.writeUInt16LE(1, 22);
	header.writeUInt32LE(SAMPLE_RATE, 24);
	header.writeUInt32LE(SAMPLE_RATE * 2, 28);
	header.writeUInt16LE(2, 32);
	header.writeUInt16LE(16, 34);
	header.write("data", 36);
	header.writeUInt32LE(data.length, 40);

	await writeFile(path, Buffer.concat([header, data]));
};

export const FAKE_MIC_ARGS = (wav: string) => [
	"--use-fake-ui-for-media-stream",
	"--use-fake-device-for-media-stream",
	`--use-file-for-fake-audio-capture=${wav}`,
	"--autoplay-policy=no-user-gesture-required",
];
