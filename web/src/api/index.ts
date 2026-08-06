import type { DebutApi } from "./DebutApi";
import { HttpDebutApi } from "./HttpDebutApi";
import { MockDebutApi } from "./MockDebutApi";

export const API_MODE =
	import.meta.env.VITE_API_MODE === "live" ? "live" : "mock";

export const api: DebutApi =
	import.meta.env.VITE_API_MODE === "live"
		? new HttpDebutApi(import.meta.env.VITE_API_BASE_URL ?? "/api/v1")
		: new MockDebutApi();

export { pollJob } from "./pollJob";
export * from "./types";
