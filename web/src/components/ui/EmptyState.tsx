import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

type Props = {
	icon: LucideIcon;
	title: string;
	body: string;
	action?: ReactNode;
};

export const EmptyState = ({ icon: Icon, title, body, action }: Props) => (
	<div className="flex flex-col items-center gap-4 rounded-md border border-border border-dashed px-6 py-14 text-center">
		<Icon size={28} strokeWidth={1.5} className="text-text-muted" aria-hidden />
		<div>
			<p className="font-medium text-base text-text">{title}</p>
			<p className="mx-auto mt-2 max-w-sm text-sm text-text-muted">{body}</p>
		</div>
		{action}
	</div>
);
