type Props = {
	level: number;
	label?: string;
	hint?: string;
};

export const LevelMeter = ({ level, label = "Microphone", hint }: Props) => {
	const clamped = Math.min(1, Math.max(0, level));
	const tooQuiet = clamped < 0.08;
	const tooLoud = clamped > 0.92;

	return (
		<div className="flex flex-col gap-2">
			<div className="flex items-baseline justify-between gap-3">
				<span className="text-sm text-text">{label}</span>
				<span
					className={`text-xs ${tooLoud ? "text-error-text" : "text-text-muted"}`}
				>
					{tooLoud ? "Too loud" : tooQuiet ? "Nothing heard" : "Good level"}
				</span>
			</div>
			<div className="h-2 overflow-hidden rounded-pill bg-surface-2">
				<div
					className={`h-full rounded-pill transition-[width] duration-100 ${
						tooLoud ? "bg-error" : "bg-accent"
					}`}
					style={{ width: `${clamped * 100}%` }}
				/>
			</div>
			<meter
				className="sr-only"
				aria-label={label}
				min={0}
				max={1}
				high={0.92}
				value={clamped}
			>
				{Math.round(clamped * 100)}%
			</meter>
			{hint ? <p className="text-xs text-text-muted">{hint}</p> : null}
		</div>
	);
};
