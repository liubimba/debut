import { createBrowserRouter } from "react-router";
import { AppShell } from "./components/shell/AppShell";
import { FeedbackScreen } from "./routes/FeedbackScreen";
import { ImportScreen } from "./routes/ImportScreen";
import { LibraryScreen } from "./routes/LibraryScreen";
import { OnboardingScreen } from "./routes/OnboardingScreen";
import { RecordScreen } from "./routes/RecordScreen";
import { SettingsScreen } from "./routes/SettingsScreen";
import { SingAlongScreen } from "./routes/SingAlongScreen";
import { SongScreen } from "./routes/SongScreen";
import { StyleguideScreen } from "./routes/StyleguideScreen";

export const router = createBrowserRouter([
	{
		element: <AppShell />,
		children: [
			{ index: true, element: <LibraryScreen /> },
			{ path: "import", element: <ImportScreen /> },
			{ path: "song/:songId", element: <SongScreen /> },
			{ path: "settings", element: <SettingsScreen /> },
			{ path: "styleguide", element: <StyleguideScreen /> },
		],
	},
	{
		element: <AppShell chrome="immersive" />,
		children: [
			{ path: "song/:songId/singalong", element: <SingAlongScreen /> },
			{ path: "song/:songId/record", element: <RecordScreen /> },
			{ path: "song/:songId/feedback", element: <FeedbackScreen /> },
			{ path: "onboarding", element: <OnboardingScreen /> },
		],
	},
]);
