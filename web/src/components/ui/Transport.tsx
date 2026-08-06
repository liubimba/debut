import { Pause, Play, Square } from "lucide-react";

type RecordButtonProps = {
	recording: boolean;
	onToggle: () => void;
	disabled?: boolean;
};

export const RecordButton = ({
	recording,
	onToggle,
	disabled,
}: RecordButtonProps) => (
	<button
		type="button"
		onClick={onToggle}
		disabled={disabled}
		className="flex items-center gap-3 rounded-pill bg-error px-6 py-4 font-medium text-base text-on-error transition-opacity duration-150 hover:opacity-90 disabled:bg-surface-2 disabled:text-text-muted"
	>
		{recording ? (
			<Square size={18} fill="currentColor" strokeWidth={0} aria-hidden />
		) : (
			<span className="size-4 rounded-pill bg-current" aria-hidden />
		)}
		{recording ? "Stop recording" : "Start recording"}
	</button>
);

type PlayButtonProps = {
	playing: boolean;
	onToggle: () => void;
	label: string;
};

export const PlayButton = ({ playing, onToggle, label }: PlayButtonProps) => (
	<button
		type="button"
		onClick={onToggle}
		className="flex items-center gap-3 rounded-pill border border-border px-5 py-3 text-sm text-text transition-colors duration-150 hover:bg-surface"
	>
		{playing ? (
			<Pause size={16} fill="currentColor" strokeWidth={0} aria-hidden />
		) : (
			<Play size={16} fill="currentColor" strokeWidth={0} aria-hidden />
		)}
		{playing ? "Pause" : label}
	</button>
);
