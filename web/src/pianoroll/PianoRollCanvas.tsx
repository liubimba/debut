import { useEffect, useRef } from "react";
import {
	PianoRollRenderer,
	type RollOptions,
	type RollSources,
} from "./PianoRollRenderer";

type Props = {
	sources: RollSources;
	options?: RollOptions;
	className?: string;
	label: string;
	revision?: string;
};

export const PianoRollCanvas = ({
	sources,
	options,
	className,
	label,
	revision = "",
}: Props) => {
	const canvasRef = useRef<HTMLCanvasElement>(null);
	const rendererRef = useRef<PianoRollRenderer | null>(null);
	const sourcesRef = useRef(sources);
	sourcesRef.current = sources;

	useEffect(() => {
		if (revision.length === 0) return;
		rendererRef.current?.requestFrame();
	}, [revision]);

	useEffect(() => {
		const canvas = canvasRef.current;
		if (!canvas) return;
		const stable: RollSources = {
			notes: () => sourcesRef.current.notes(),
			positionSec: () => sourcesRef.current.positionSec(),
			frames: () => sourcesRef.current.frames(),
			trace: () => sourcesRef.current.trace(),
			region: () => sourcesRef.current.region(),
			minConfidence: () => sourcesRef.current.minConfidence(),
			running: () => sourcesRef.current.running(),
			onNote: () => sourcesRef.current.onNote(),
		};
		const renderer = options
			? new PianoRollRenderer(canvas, stable, options)
			: new PianoRollRenderer(canvas, stable);
		rendererRef.current = renderer;
		const wake = () => renderer.requestFrame();
		document.addEventListener("visibilitychange", wake);
		return () => {
			document.removeEventListener("visibilitychange", wake);
			renderer.dispose();
			rendererRef.current = null;
		};
	}, [options]);

	return (
		<canvas
			ref={canvasRef}
			role="img"
			aria-label={label}
			className={className ?? "block h-full w-full rounded-md"}
		/>
	);
};
