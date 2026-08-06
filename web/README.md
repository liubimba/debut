# Debut — web design prototype

A clickable prototype of the Debut vocal trainer. It exists so the design can be judged in a
browser and so the sing-along loop is proven before it is rewritten in Kotlin/Compose. It is a
design artefact, not the shipping frontend.

Two things in here are real rather than mocked:

- **the sing-along loop** — a genuine microphone, YIN pitch detection in an AudioWorklet, and a
  backing track that actually stops when you drift off the note;
- **the backend contract** — the same calling code runs against the mock and against the FastAPI
  service in `services/backend`.

## Running it

```bash
nvm use 24          # the system default is Node 16; Biome needs structuredClone
npm install
npm run dev         # mock backend, http://localhost:5173
npm run dev:live    # against a real backend on :4999, proxied through Vite
```

`npm run dev:live` needs `debut-backend --port 4999` running. Requests go through the Vite proxy,
so `VITE_API_BASE_URL` stays the relative `/api/v1` in both modes and CORS never comes up.

## Gates

```bash
npm run verify      # everything below, in order
```

| Script | What only it catches |
|---|---|
| `typecheck` | Vite never typechecks — this is the only gate that sees a type error |
| `lint` | Biome, config inherited from the repo root |
| `test` | YIN accuracy, the octave guard, sing-along hysteresis, roll geometry, the WAV encoder |
| `build` | unknown Tailwind v4 utilities surface here and nowhere else |
| `verify:pages` | console errors, failed requests, horizontal overflow at 390px, empty CSS variables, a blank root |
| `verify:contrast` | every text and fill token against every surface, in **both** themes, read from the live page |
| `verify:perf` | p95 frame time under 4x CPU throttle, long tasks, JS weight |
| `verify:singalong` | the whole loop through a fake capture device: the mic check gates the start, the music stops on a sustained miss and resumes on return |

Everything browser-based runs against `vite preview` on port 4173 with `--strictPort`. Measuring
the dev server is meaningless and strict-port failure is the only thing stopping a stale server
from answering for a build you did not make.

Screenshots land in `screenshots/` (gitignored). `shoot-async.ts` captures screens that need time
to settle or a disclosure opened; `shoot-live.ts` drives the sing-along with a fake microphone to
capture the on-note and stopped states.

## Layout

```
src/
  api/          DebutApi interface, HTTP client, in-memory mock, job polling
  audio/        engine — dsp/, singalong/, worklet/, capture/, transport/, react/
  components/   ui/ primitives and practice/ helpers
  fixtures/     note tracks plus a synthesiser that renders playable stems
  pianoroll/    Canvas 2D renderer, viewport maths, trace ring
  routes/       the screens
  styles/       theme.css (semantics per theme) and app.css (@theme inline)
  theme/        theme hook, paired with the boot script in index.html
scripts/        gates and screenshot drivers
```

## Things that will bite

- **`@theme inline` does not emit `--color-*` into `:root`.** Anything reading a variable by hand —
  inline styles, canvas — must use the semantic names from `theme.css`.
- **`color-mix()` is unreliable as a canvas `fillStyle`.** Use `ctx.globalAlpha` instead.
- **Install packages with the dev server stopped.** Vite keeps a dependency-optimizer cache; a stale
  one answers 504 for every chunk and the page goes blank.
- **No named functions inside `page.evaluate`.** tsx injects esbuild's `__name` helper, which does
  not exist in the browser context.
- **Never chain `build` behind `lint` with `&&` and swallow the output.** A lint failure silently
  skips the build and you end up screenshotting the previous bundle.
- **`getUserMedia` needs a secure context.** `localhost` is exempt, `http://192.168.x.x` is not — a
  phone test needs `vite --host` plus a TLS plugin or a tunnel.
