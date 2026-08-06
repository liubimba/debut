import { Outlet } from "react-router";
import { NavRail } from "./NavRail";
import { NavTabBar } from "./NavTabBar";
import { Library, Settings } from "./navIcons";
import { ThemeToggle } from "./ThemeToggle";

export type NavDestination = {
	to: string;
	label: string;
	icon: typeof Library;
};

const destinations: NavDestination[] = [
	{ to: "/", label: "My songs", icon: Library },
	{ to: "/settings", label: "Settings", icon: Settings },
];

type Props = {
	chrome?: "standard" | "immersive";
};

export const AppShell = ({ chrome = "standard" }: Props) => {
	if (chrome === "immersive") {
		return (
			<div className="min-h-dvh bg-bg text-text">
				<Outlet />
			</div>
		);
	}

	return (
		<div className="min-h-dvh bg-bg text-text md:flex">
			<NavRail destinations={destinations} />
			<div className="flex min-w-0 flex-1 flex-col">
				<header className="flex items-center justify-between px-5 pt-5 pb-2 md:px-10 md:pt-8">
					<span className="font-semibold text-sm tracking-tight">Debut</span>
					<ThemeToggle />
				</header>
				<main className="min-w-0 flex-1 px-5 pb-[calc(80px+env(safe-area-inset-bottom))] md:px-10 md:pb-10">
					<Outlet />
				</main>
			</div>
			<NavTabBar destinations={destinations} />
		</div>
	);
};
