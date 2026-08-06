import { NavLink } from "react-router";
import type { NavDestination } from "./AppShell";

type Props = {
	destinations: NavDestination[];
};

export const NavRail = ({ destinations }: Props) => (
	<nav
		aria-label="Primary"
		className="hidden w-52 shrink-0 flex-col gap-1 border-border border-r p-4 md:flex"
	>
		{destinations.map(({ to, label, icon: Icon }) => (
			<NavLink
				key={to}
				to={to}
				end={to === "/"}
				className={({ isActive }) =>
					`flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition-colors duration-150 ${
						isActive
							? "bg-surface font-medium text-text"
							: "text-text-muted hover:text-text"
					}`
				}
			>
				<Icon size={18} strokeWidth={1.75} aria-hidden />
				{label}
			</NavLink>
		))}
	</nav>
);
