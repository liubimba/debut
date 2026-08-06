import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "./cn";

type Props = HTMLAttributes<HTMLDivElement> & {
	children: ReactNode;
};

export const Card = ({ className, children, ...rest }: Props) => (
	<div
		className={cn("rounded-md border border-border bg-surface p-5", className)}
		{...rest}
	>
		{children}
	</div>
);

export const CardLabel = ({ children }: { children: ReactNode }) => (
	<p className="font-medium text-sm text-text">{children}</p>
);
