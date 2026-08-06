import { cn } from "./cn";

type Props = {
	className?: string;
};

export const Skeleton = ({ className }: Props) => (
	<span
		aria-hidden
		className={cn(
			"block animate-pulse rounded-sm bg-surface-2 motion-reduce:animate-none",
			className,
		)}
	/>
);
