import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "./cn";

type Variant = "primary" | "secondary" | "quiet";
type Size = "hero" | "default";

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
	variant?: Variant;
	size?: Size;
	children: ReactNode;
};

const variants: Record<Variant, string> = {
	primary:
		"bg-accent text-on-accent enabled:hover:opacity-90 disabled:bg-surface-2 disabled:text-text-muted",
	secondary:
		"border border-border text-text enabled:hover:bg-surface disabled:text-text-muted",
	quiet: "text-text-muted underline underline-offset-4 enabled:hover:text-text",
};

const sizes: Record<Size, string> = {
	hero: "h-14 w-full px-6 text-base font-medium",
	default: "h-11 px-5 text-sm",
};

export const Button = ({
	variant = "primary",
	size = "default",
	className,
	children,
	...rest
}: Props) => (
	<button
		type="button"
		className={cn(
			"inline-flex items-center justify-center gap-2 rounded-md transition-opacity duration-150",
			variant === "quiet" ? "h-11 px-2 text-sm" : sizes[size],
			variants[variant],
			className,
		)}
		{...rest}
	>
		{children}
	</button>
);
