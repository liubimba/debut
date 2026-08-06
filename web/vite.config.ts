import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: {
		port: 5173,
		strictPort: true,
		proxy: {
			"/api": {
				target: "http://localhost:4999",
				changeOrigin: true,
			},
		},
	},
	preview: {
		port: 4173,
		strictPort: true,
	},
	build: {
		target: "es2022",
	},
});
