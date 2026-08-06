import { NavLink } from "react-router";
import type { NavDestination } from "./AppShell";

type Props = {
	destinations: NavDestination[];
};

export const NavTabBar = ({ destinations }: Props) => (
	<nav
		aria-label="Primary"
		className="fixed inset-x-0 bottom-0 z-10 flex border-border border-t bg-bg pb-[env(safe-area-inset-bottom)] md:hidden"
	>
		{destinations.map(({ to, label, icon: Icon }) => (
			<NavLink
				key={to}
				to={to}
				end={to === "/"}
				className={({ isActive }) =>
					`flex min-h-[60px] flex-1 flex-col items-center justify-center gap-1 text-xs transition-colors duration-150 ${
						isActive ? "font-medium text-text" : "text-text-muted"
					}`
				}
			>
				<Icon size={20} strokeWidth={1.75} aria-hidden />
				{label}
			</NavLink>
		))}
	</nav>
);
