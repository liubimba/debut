import type { LucideIcon } from "lucide-react";
import { Headphones, Mic, Music } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/Button";

type Step = {
	icon: LucideIcon;
	title: string;
	body: string;
	action: string;
};

const STEPS: Step[] = [
	{
		icon: Music,
		title: "Debut takes the singer out of a song",
		body: "You get the music on its own, and the melody drawn on screen so you can see which note comes next.",
		action: "Next",
	},
	{
		icon: Mic,
		title: "It listens while you sing",
		body: "The music keeps playing as long as you are on the right note. Drift off and it stops until you find the note again.",
		action: "Next",
	},
	{
		icon: Headphones,
		title: "Wear headphones",
		body: "Through speakers the microphone hears the music as well as you, and Debut cannot tell the two apart.",
		action: "Start",
	},
];

export const OnboardingScreen = () => {
	const navigate = useNavigate();
	const [index, setIndex] = useState(0);
	const step = STEPS[index];
	if (!step) return null;
	const Icon = step.icon;

	return (
		<section className="mx-auto flex min-h-dvh max-w-md flex-col justify-between gap-10 px-6 py-[calc(2.5rem+env(safe-area-inset-top))]">
			<div
				className="flex gap-2"
				role="progressbar"
				aria-label="Getting started"
				aria-valuemin={1}
				aria-valuemax={STEPS.length}
				aria-valuenow={index + 1}
			>
				{STEPS.map((item, position) => (
					<span
						key={item.title}
						className={`h-1 flex-1 rounded-pill ${position <= index ? "bg-accent" : "bg-surface-2"}`}
					/>
				))}
			</div>

			<div className="flex flex-col gap-6">
				<span className="flex size-14 items-center justify-center rounded-md bg-surface text-text-muted">
					<Icon size={26} strokeWidth={1.5} aria-hidden />
				</span>
				<div>
					<h1 className="font-semibold text-2xl tracking-tight">
						{step.title}
					</h1>
					<p className="mt-3 text-text-muted">{step.body}</p>
				</div>
			</div>

			<div className="flex flex-col gap-2 pb-[env(safe-area-inset-bottom)]">
				<Button
					variant="primary"
					size="hero"
					onClick={() => {
						if (index + 1 < STEPS.length) setIndex(index + 1);
						else navigate("/");
					}}
				>
					{step.action}
				</Button>
				<Button variant="quiet" onClick={() => navigate("/")}>
					Skip
				</Button>
			</div>
		</section>
	);
};
