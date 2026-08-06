import { useState } from "react";
import { Button } from "../components/ui/Button";
import { Card, CardLabel } from "../components/ui/Card";
import { LevelMeter } from "../components/ui/LevelMeter";
import { PitchIndicator } from "../components/ui/PitchIndicator";
import { Skeleton } from "../components/ui/Skeleton";
import { Slider } from "../components/ui/Slider";
import { type Region, Timeline } from "../components/ui/Timeline";
import { PlayButton, RecordButton } from "../components/ui/Transport";

const TOKENS = [
	["bg", "surface", "surface-2", "border"],
	["text", "text-muted", "accent", "accent-text"],
	["error", "error-text", "note", "note-active"],
];

const TYPE = [
	{ name: "3xl", className: "text-3xl font-semibold", sample: "A4" },
	{
		name: "2xl",
		className: "text-2xl font-semibold tracking-tight",
		sample: "My songs",
	},
	{
		name: "xl",
		className: "text-xl font-semibold tracking-tight",
		sample: "Screen heading",
	},
	{ name: "lg", className: "text-lg font-medium", sample: "Section heading" },
	{
		name: "base",
		className: "text-base",
		sample: "Body copy, the default size",
	},
	{
		name: "sm",
		className: "text-sm text-text-muted",
		sample: "Secondary copy and hints",
	},
	{
		name: "xs",
		className: "text-xs text-text-muted",
		sample: "Timestamps and captions",
	},
];

const peaks = Array.from({ length: 140 }, (_, i) => {
	const envelope = Math.sin((i / 140) * Math.PI);
	const grain = Math.abs(Math.sin(i * 1.7) * Math.cos(i * 0.31));
	return 0.1 + envelope * grain * 0.9;
});

const Swatch = ({ token }: { token: string }) => (
	<div className="flex items-center gap-3">
		<span
			className="size-9 shrink-0 rounded-sm border border-border"
			style={{ backgroundColor: `var(--${token})` }}
		/>
		<span className="text-sm text-text-muted">{token}</span>
	</div>
);

const Section = ({
	title,
	children,
}: {
	title: string;
	children: React.ReactNode;
}) => (
	<section className="flex flex-col gap-4">
		<h2 className="border-border border-b pb-2 font-medium text-lg">{title}</h2>
		{children}
	</section>
);

export const StyleguideScreen = () => {
	const [value, setValue] = useState(50);
	const [recording, setRecording] = useState(false);
	const [playing, setPlaying] = useState(false);
	const [region, setRegion] = useState<Region | null>({ start: 20, end: 60 });

	return (
		<div className="mx-auto flex w-full max-w-3xl flex-col gap-10 pb-10">
			<header>
				<h1 className="font-semibold text-2xl tracking-tight">Style</h1>
				<p className="mt-2 text-sm text-text-muted">
					Neutral greys, one accent for actions, one red for what went wrong.
					System font. Nothing else.
				</p>
			</header>

			<Section title="Colour">
				{TOKENS.map((row) => (
					<div
						key={row.join()}
						className="grid grid-cols-2 gap-3 sm:grid-cols-4"
					>
						{row.map((token) => (
							<Swatch key={token} token={token} />
						))}
					</div>
				))}
			</Section>

			<Section title="Type">
				<div className="flex flex-col gap-3">
					{TYPE.map(({ name, className, sample }) => (
						<div key={name} className="flex items-baseline gap-6">
							<span className="w-12 shrink-0 text-text-muted text-xs">
								{name}
							</span>
							<span className={className}>{sample}</span>
						</div>
					))}
				</div>
			</Section>

			<Section title="Buttons">
				<div className="flex flex-wrap items-center gap-3">
					<Button variant="primary">Sing along</Button>
					<Button variant="secondary">Record a take</Button>
					<Button variant="quiet">Skip</Button>
					<Button variant="primary" disabled>
						Disabled
					</Button>
				</div>
				<div className="max-w-sm">
					<Button variant="primary" size="hero">
						Start the song
					</Button>
				</div>
			</Section>

			<Section title="Pitch">
				<div className="grid gap-6 sm:grid-cols-3">
					<Card>
						<PitchIndicator
							noteName="A4"
							cents={8}
							state="in-note"
							tolerance={50}
						/>
					</Card>
					<Card>
						<PitchIndicator
							noteName="A4"
							cents={-72}
							state="flat"
							tolerance={50}
						/>
					</Card>
					<Card>
						<PitchIndicator
							noteName="A4"
							cents={0}
							state="silent"
							tolerance={50}
						/>
					</Card>
				</div>
			</Section>

			<Section title="Transport and waveform">
				<Timeline
					peaks={peaks}
					duration={140}
					position={48}
					region={region}
					onRegionChange={setRegion}
				/>
				<div className="flex flex-wrap items-center gap-4">
					<RecordButton
						recording={recording}
						onToggle={() => setRecording(!recording)}
					/>
					<PlayButton
						playing={playing}
						onToggle={() => setPlaying(!playing)}
						label="Listen to your take"
					/>
				</div>
			</Section>

			<Section title="Input">
				<div className="grid gap-6 sm:grid-cols-2">
					<Card className="flex flex-col gap-5">
						<CardLabel>Slider</CardLabel>
						<Slider
							label="How far off counts as wrong"
							value={value}
							min={10}
							max={100}
							step={5}
							unit=" cents"
							hint="100 cents is one semitone."
							onChange={setValue}
						/>
					</Card>
					<Card className="flex flex-col gap-5">
						<CardLabel>Level</CardLabel>
						<LevelMeter level={value / 100} />
					</Card>
				</div>
			</Section>

			<Section title="Loading">
				<Card className="flex flex-col gap-3">
					<Skeleton className="h-4 w-40" />
					<Skeleton className="h-3 w-full" />
					<Skeleton className="h-3 w-2/3" />
				</Card>
			</Section>
		</div>
	);
};
