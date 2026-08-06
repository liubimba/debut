import { ChevronDown } from "lucide-react";
import { useState } from "react";
import { API_MODE } from "../api";
import { DEFAULT_CONFIG, LIMITS } from "../audio/config";
import { Card, CardLabel } from "../components/ui/Card";
import { Slider } from "../components/ui/Slider";
import { useTheme } from "../theme/useTheme";

const DEVICES = ["Built-in microphone", "USB microphone", "Headset microphone"];

const Choice = <T extends string>({
	label,
	options,
	value,
	onChange,
}: {
	label: string;
	options: readonly T[];
	value: T;
	onChange: (value: T) => void;
}) => (
	<div className="flex flex-col gap-2">
		<span className="text-sm text-text">{label}</span>
		<div className="flex gap-2">
			{options.map((option) => (
				<button
					key={option}
					type="button"
					onClick={() => onChange(option)}
					aria-pressed={value === option}
					className={`h-10 flex-1 rounded-md text-sm capitalize transition-colors duration-150 ${
						value === option
							? "bg-accent text-on-accent"
							: "border border-border text-text-muted hover:text-text"
					}`}
				>
					{option}
				</button>
			))}
		</div>
	</div>
);

export const SettingsScreen = () => {
	const { mode, choose } = useTheme();
	const [device, setDevice] = useState(DEVICES[0] ?? "");
	const [strictness, setStrictness] = useState<"gentle" | "normal" | "strict">(
		"normal",
	);
	const [advanced, setAdvanced] = useState(false);
	const [tolerance, setTolerance] = useState(DEFAULT_CONFIG.enterCents);
	const [exitHold, setExitHold] = useState(DEFAULT_CONFIG.exitHoldMs);
	const [latency, setLatency] = useState(DEFAULT_CONFIG.latencyCompensationMs);
	const [host, setHost] = useState("localhost:4999");

	return (
		<section className="mx-auto flex w-full max-w-xl flex-col gap-6 pb-8">
			<header>
				<h1 className="font-semibold text-2xl tracking-tight">Settings</h1>
			</header>

			<Card className="flex flex-col gap-5">
				<CardLabel>Microphone</CardLabel>
				<label className="flex flex-col gap-2">
					<span className="text-sm text-text-muted">
						Which one to listen to
					</span>
					<select
						value={device}
						onChange={(event) => setDevice(event.target.value)}
						className="h-11 rounded-md border border-border bg-bg px-3 text-sm text-text"
					>
						{DEVICES.map((option) => (
							<option key={option} value={option}>
								{option}
							</option>
						))}
					</select>
				</label>
			</Card>

			<Card className="flex flex-col gap-5">
				<CardLabel>How strict Debut is</CardLabel>
				<Choice
					label="When singing along"
					options={["gentle", "normal", "strict"] as const}
					value={strictness}
					onChange={setStrictness}
				/>
				<p className="text-sm text-text-muted">
					{strictness === "gentle"
						? "The music keeps going unless you are clearly on a different note."
						: strictness === "strict"
							? "The music stops as soon as you slip. Good once the song is under your belt."
							: "The music stops when you drift off and stay off. Start here."}
				</p>
			</Card>

			<Card className="flex flex-col gap-5">
				<CardLabel>Appearance</CardLabel>
				<Choice
					label="Theme"
					options={["system", "light", "dark"] as const}
					value={mode}
					onChange={choose}
				/>
			</Card>

			<div className="border-border border-t pt-4">
				<button
					type="button"
					onClick={() => setAdvanced(!advanced)}
					aria-expanded={advanced}
					className="flex w-full items-center justify-between text-sm text-text-muted transition-colors duration-150 hover:text-text"
				>
					Advanced
					<ChevronDown
						size={16}
						strokeWidth={1.75}
						className={`transition-transform duration-200 ${advanced ? "rotate-180" : ""}`}
						aria-hidden
					/>
				</button>

				{advanced ? (
					<div className="mt-4 flex flex-col gap-5">
						<Card className="flex flex-col gap-5">
							<CardLabel>Tuning</CardLabel>
							<Slider
								label="How far off counts as wrong"
								value={tolerance}
								min={LIMITS.enterCents[0]}
								max={LIMITS.enterCents[1]}
								step={5}
								unit=" cents"
								hint="100 cents is one semitone."
								onChange={setTolerance}
							/>
							<Slider
								label="How long you can drift before it stops"
								value={exitHold}
								min={LIMITS.exitHoldMs[0]}
								max={LIMITS.exitHoldMs[1]}
								step={20}
								unit=" ms"
								hint="Too short and it stops on every wobble; too long and whole phrases slide by."
								onChange={setExitHold}
							/>
							<Slider
								label="Delay between your voice and the app"
								value={latency}
								min={LIMITS.latencyCompensationMs[0]}
								max={LIMITS.latencyCompensationMs[1]}
								step={10}
								unit=" ms"
								hint="Hold a steady note and nudge this until the line sits on the target."
								onChange={setLatency}
							/>
						</Card>

						<Card className="flex flex-col gap-3">
							<CardLabel>Engine</CardLabel>
							<label className="flex flex-col gap-2">
								<span className="text-sm text-text-muted">
									Where the engine runs
								</span>
								<input
									value={host}
									onChange={(event) => setHost(event.target.value)}
									spellCheck={false}
									className="numeric h-11 rounded-md border border-border bg-bg px-3 text-sm text-text"
								/>
							</label>
							<p className="text-sm text-text-muted">
								Splitting a song is heavy work and runs on a computer, not on
								your phone. A phone connects to it over your home network.
							</p>
							<p className="text-sm text-text-muted">
								Currently using: <span className="text-text">{API_MODE}</span>{" "}
								data
							</p>
						</Card>
					</div>
				) : null}
			</div>
		</section>
	);
};
