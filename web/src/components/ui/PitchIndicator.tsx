import { ArrowDown, ArrowUp, Check, Mic } from "lucide-react";

export type HitState = "in-note" | "sharp" | "flat" | "silent";

type Props = {
	noteName: string;
	cents: number;
	state: HitState;
	tolerance: number;
};

const GUIDE: Record<
	HitState,
	{ icon: typeof Check; say: string; tone: string }
> = {
	"in-note": { icon: Check, say: "Hold it", tone: "text-accent-text" },
	sharp: { icon: ArrowDown, say: "Sing lower", tone: "text-error-text" },
	flat: { icon: ArrowUp, say: "Sing higher", tone: "text-error-text" },
	silent: { icon: Mic, say: "Sing", tone: "text-text-muted" },
};

export const PitchIndicator = ({
	noteName,
	cents,
	state,
	tolerance,
}: Props) => {
	const guide = GUIDE[state];
	const Icon = guide.icon;
	const span = 120;
	const offset = Math.max(-span, Math.min(span, cents));
	const needleLeft = 50 + (offset / span) * 50;
	const bandWidth = (tolerance / span) * 50;

	return (
		<div className="flex w-full max-w-sm flex-col items-center gap-4">
			<p className="text-sm text-text-muted">Sing this note</p>

			<p className="numeric font-semibold text-3xl text-text tabular-nums">
				{noteName}
			</p>

			<div className="relative h-3 w-full overflow-hidden rounded-pill bg-surface-2">
				<span
					className="absolute inset-y-0 rounded-pill bg-accent/20"
					style={{ left: `${50 - bandWidth}%`, width: `${bandWidth * 2}%` }}
				/>
				{state === "silent" ? null : (
					<span
						className={`absolute inset-y-0 w-1.5 rounded-pill transition-[left] duration-100 ${
							state === "in-note" ? "bg-accent" : "bg-error"
						}`}
						style={{ left: `calc(${needleLeft}% - 3px)` }}
					/>
				)}
			</div>

			<p
				className={`flex items-center gap-2 font-medium text-lg ${guide.tone}`}
				aria-live="polite"
			>
				<Icon size={20} strokeWidth={2.5} aria-hidden />
				{guide.say}
			</p>
		</div>
	);
};
