import { useMemo } from "react";
import type { Note } from "../../audio/types";

export const usePeaks = (
	notes: Note[],
	durationSec: number,
	resolution = 180,
) =>
	useMemo(() => {
		const peaks = new Array<number>(resolution).fill(0.06);
		if (durationSec <= 0) return peaks;

		for (const note of notes) {
			const from = Math.floor((note.start_time / durationSec) * resolution);
			const to = Math.ceil((note.end_time / durationSec) * resolution);
			for (
				let index = Math.max(0, from);
				index < Math.min(resolution, to);
				index += 1
			) {
				const phase = (index - from) / Math.max(1, to - from);
				peaks[index] = 0.35 + 0.55 * Math.sin(Math.PI * phase) ** 0.6;
			}
		}
		return peaks;
	}, [notes, durationSec, resolution]);
