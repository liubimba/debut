import { Moon, Sun } from "lucide-react";
import { useTheme } from "../../theme/useTheme";

export const ThemeToggle = () => {
	const { resolved, toggle } = useTheme();
	const Icon = resolved === "dark" ? Sun : Moon;

	return (
		<button
			type="button"
			onClick={toggle}
			aria-label={
				resolved === "dark" ? "Switch to light theme" : "Switch to dark theme"
			}
			className="hit-44 flex size-9 items-center justify-center rounded-md text-text-muted transition-colors duration-150 hover:text-text"
		>
			<Icon size={18} strokeWidth={1.75} aria-hidden />
		</button>
	);
};
