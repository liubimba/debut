import { useId } from "react";

type Props = {
	label: string;
	value: number;
	min: number;
	max: number;
	step?: number;
	unit?: string;
	hint?: string;
	onChange: (value: number) => void;
};

export const Slider = ({
	label,
	value,
	min,
	max,
	step = 1,
	unit,
	hint,
	onChange,
}: Props) => {
	const id = useId();
	const filled = ((value - min) / (max - min)) * 100;

	return (
		<div className="flex flex-col gap-1">
			<div className="flex items-baseline justify-between gap-3">
				<label htmlFor={id} className="text-sm text-text">
					{label}
				</label>
				<span className="numeric text-sm text-text-muted">
					{value}
					{unit}
				</span>
			</div>
			<input
				id={id}
				type="range"
				min={min}
				max={max}
				step={step}
				value={value}
				onChange={(event) => onChange(Number(event.target.value))}
				className="h-11 w-full cursor-pointer appearance-none bg-transparent [&::-moz-range-thumb]:size-5 [&::-moz-range-thumb]:cursor-pointer [&::-moz-range-thumb]:rounded-pill [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:bg-accent [&::-webkit-slider-thumb]:size-5 [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-pill [&::-webkit-slider-thumb]:bg-accent"
				style={{
					background: `linear-gradient(to right, var(--accent) ${filled}%, var(--surface-2) ${filled}%) center/100% 4px no-repeat`,
				}}
			/>
			{hint ? <p className="text-xs text-text-muted">{hint}</p> : null}
		</div>
	);
};
