# RedactGuard Android

**Find and remove sensitive information from documents without sending the original content to the cloud.**

RedactGuard imports a PDF or text, detects the PII you care about with local AI, lets you review every match and exports a redacted PDF.

[Mission](https://daniele21.github.io/) · [Why](#why-redactguard-exists) · [What it does](#what-you-can-do-today) · [How to use it](#how-to-use-it) · [How it works](#how-it-works) · [Status](#current-status-and-limits) · [Docs](#documentation)

## Why RedactGuard exists

I'm exploring [how much AI can move from the cloud to infrastructure and devices we control](https://daniele21.github.io/), and where Local, Hybrid or Cloud actually makes sense.

RedactGuard tests a concrete privacy-sensitive use case:

> **Can sensitive documents be processed locally before they are shared?**

The idea is simple: use local AI for detection, keep a human in control of the final decision, and export only the minimized document.

RedactGuard is not a compliance guarantee. It is a tool for reducing unnecessary exposure of sensitive information.

## What you can do today

You can:

- import a PDF or paste text;
- choose built-in or custom PII types to look for;
- run the analysis through a separately installed Harnex host;
- review detected values with sensitive content hidden by default;
- confirm or reject findings before redaction;
- export a new redacted PDF through Android's system file flow;
- inspect privacy-safe diagnostics when local AI is unavailable or fails.

RedactGuard owns the document workflow and redaction policy. It does **not** package an LLM runtime, GGUF models or `llama.cpp`.

## How to use it

### As a user

The main flow is:

```text
Import PDF / paste text
        |
        v
Choose what to protect
        |
        v
Run local analysis
        |
        v
Review the findings
        |
        v
Confirm redactions
        |
        v
Export the protected PDF
```

For AI analysis, a compatible **Harnex** host must be installed, configured and ready on the same Android device.

1. Open RedactGuard and add a PDF or paste text.
2. Choose the PII types you want to detect.
3. Start the analysis.
4. Review each finding and keep only the redactions you want.
5. Export the new PDF.

The original document stays inside the local product boundary by default. There is no silent cloud fallback.

### As a developer

Prerequisites are the Android/JDK toolchain required by the repository plus a connected emulator or device.

```bash
git clone https://github.com/daniele21/redactguard-android.git
cd redactguard-android

./gradlew --version
bash scripts/doctor-android.sh
./gradlew :app:installDebug
```

For the full Harnex + RedactGuard two-app setup and device evidence flow, see [`docs/evidence/physical-two-apk.md`](docs/evidence/physical-two-apk.md).

## How it works

```text
RedactGuard
  document workflow
  PII policy
  human review
  redaction / export
        |
        v
Harnex Consumer Android SDK
        |
        v
Binder
        |
        v
Harnex host
  model + runtime ownership
        |
        v
Local model
```

This split is deliberate:

- **RedactGuard owns the user problem.** Documents, PII policy, review and export stay here.
- **Harnex owns local AI infrastructure.** Models, runtime lifecycle, scheduling and residency stay there.
- **The boundary is explicit.** RedactGuard does not silently choose a model or bypass the host when local AI fails.
- **Sensitive data stays local by default.** Diagnostics do not need the raw document content to explain a failure.

See [`docs/architecture.md`](docs/architecture.md) for the full ownership model.

## Current status and limits

RedactGuard is an active Android product under integration on `dev`.

The current product includes PDF and pasted-text ingestion, built-in/custom PII selection, local analysis, masked review, fail-closed redaction/export and adaptive Android UI.

Important current limits:

- OCR and VLM document understanding are out of scope;
- there is no cloud parsing fallback;
- RedactGuard does not own model selection, GGUF files or Harnex administration;
- some Harnex/RedactGuard lifecycle and same-signer real-device evidence is still being completed;
- emulator evidence is not treated as proof of production ARM64/model/OEM behavior.

See [`docs/current-state.md`](docs/current-state.md) for the exact integrated state and open blockers.

## Documentation

| Need | Start here |
| --- | --- |
| Current state | [`docs/current-state.md`](docs/current-state.md) |
| Architecture | [`docs/architecture.md`](docs/architecture.md) |
| Harnex + RedactGuard device evidence | [`docs/evidence/physical-two-apk.md`](docs/evidence/physical-two-apk.md) |
| Play internal testing | [`docs/release/play-internal-testing.md`](docs/release/play-internal-testing.md) |
| Active work | [`docs/workstreams/`](docs/workstreams/) |

## Develop and validate

Contributors work from `dev` and follow [`AGENTS.md`](AGENTS.md). Canonical commands live in [`.engineering/commands.json`](.engineering/commands.json).

The normal development install is:

```bash
./gradlew :app:installDebug
```

Use the narrowest validation that covers the change. Cross-app Binder, privacy, packaging and release claims require stronger evidence than a normal UI or domain edit.

## License and project context

RedactGuard is part of a broader Local AI effort by [Daniele Moltisanti](https://daniele21.github.io/): build real privacy-sensitive workflows, measure their limits, and decide where Local, Hybrid or Cloud makes sense.
