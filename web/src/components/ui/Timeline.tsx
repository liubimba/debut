import { useCallback, useMemo, useRef } from "react";

export type Region = {
	start: number;
	end: number;
};

type Props = {
	peaks: number[];
	duration: number;
	position: number;
	region: Region | null;
	onRegionChange?: (region: Region) => void;
	onSeek?: (seconds: number) => void;
};

const clamp01 = (value: number) => Math.min(1, Math.max(0, value));

export const formatTime = (seconds: number) => {
	const total = Math.max(0, Math.round(seconds));
	return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
};

const buildWavePaths = (
	peaks: number[],
	region: Region | null,
	duration: number,
) => {
	let litPath = "";
	let dimPath = "";
	for (let index = 0; index < peaks.length; index += 1) {
		const height = Math.max(3, (peaks[index] ?? 0) * 88);
		const fraction = index / peaks.length;
		const lit =
			region === null ||
			(fraction >= region.start / duration &&
				fraction <= region.end / duration);
		const bar = `M${index + 0.2} ${50 - height / 2}h0.6v${height}h-0.6z`;
		if (lit) litPath += bar;
		else dimPath += bar;
	}
	return { litPath, dimPath };
};

export const Timeline = ({
	peaks,
	duration,
	position,
	region,
	onRegionChange,
	onSeek,
}: Props) => {
	const trackRef = useRef<HTMLDivElement>(null);
	const dragAnchor = useRef<number | null>(null);

	const fractionAt = useCallback((clientX: number) => {
		const box = trackRef.current?.getBoundingClientRect();
		if (!box) return 0;
		return clamp01((clientX - box.left) / box.width);
	}, []);

	const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
		const fraction = fractionAt(event.clientX);
		if (!onRegionChange) {
			onSeek?.(fraction * duration);
			return;
		}
		event.currentTarget.setPointerCapture(event.pointerId);
		dragAnchor.current = fraction;
		onRegionChange({ start: fraction * duration, end: fraction * duration });
	};

	const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
		const anchor = dragAnchor.current;
		if (anchor === null || !onRegionChange) return;
		const fraction = fractionAt(event.clientX);
		onRegionChange({
			start: Math.min(anchor, fraction) * duration,
			end: Math.max(anchor, fraction) * duration,
		});
	};

	const handlePointerUp = () => {
		dragAnchor.current = null;
	};

	const regionLeft = region ? (region.start / duration) * 100 : 0;
	const regionWidth = region
		? ((region.end - region.start) / duration) * 100
		: 0;
	const playheadLeft = (clamp01(position / duration) * 100).toFixed(3);
	const { litPath, dimPath } = useMemo(
		() => buildWavePaths(peaks, region, duration),
		[peaks, region, duration],
	);

	return (
		<div className="flex flex-col gap-2">
			<div
				ref={trackRef}
				onPointerDown={handlePointerDown}
				onPointerMove={handlePointerMove}
				onPointerUp={handlePointerUp}
				className="relative h-14 touch-none select-none overflow-hidden rounded-sm bg-surface"
			>
				{region && regionWidth > 0 ? (
					<span
						className="absolute inset-y-0 bg-accent/10 ring-1 ring-accent/40"
						style={{ left: `${regionLeft}%`, width: `${regionWidth}%` }}
					/>
				) : null}
				<svg
					className="absolute inset-0 h-full w-full"
					viewBox={`0 0 ${peaks.length} 100`}
					preserveAspectRatio="none"
					aria-hidden
					focusable="false"
				>
					<title>Waveform</title>
					<path d={dimPath} fill="var(--surface-2)" />
					<path d={litPath} fill="var(--note)" />
				</svg>
				<span
					className="pointer-events-none absolute inset-y-0 w-0.5 bg-accent"
					style={{ left: `${playheadLeft}%` }}
				/>
			</div>
			<div className="flex justify-between text-text-muted text-xs">
				<span className="numeric">{formatTime(position)}</span>
				<span className="numeric">{formatTime(duration)}</span>
			</div>
		</div>
	);
};
