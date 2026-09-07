<p align="center">
  <img src="app/src/main/res/drawable-nodpi/redactguard_android_shield.png" alt="RedactGuard" width="96" />
</p>

<h1 align="center">RedactGuard Android</h1>

<p align="center">
  <strong>Protect sensitive documents with Local AI before they leave your device.</strong>
</p>

<p align="center">
  Detect PII locally through <a href="https://github.com/daniele21/android-local-llm-harness">Harnex</a>, review every finding, and export only the redacted document — with no silent cloud fallback.
</p>

<p align="center">
  <a href="https://github.com/daniele21/redactguard-android/actions/workflows/validate.yml"><img src="https://github.com/daniele21/redactguard-android/actions/workflows/validate.yml/badge.svg?branch=main" alt="Validate" /></a>
  <img src="https://img.shields.io/badge/Android-Kotlin-informational" alt="Android Kotlin" />
  <img src="https://img.shields.io/badge/AI-local--first-informational" alt="Local-first AI" />
  <img src="https://img.shields.io/badge/privacy-fail--closed-informational" alt="Fail-closed privacy" />
</p>

<p align="center">
  <a href="#try-it">Try it</a> ·
  <a href="#how-it-works">Architecture</a> ·
  <a href="#measured-not-assumed">Evidence</a> ·
  <a href="https://github.com/daniele21/android-local-llm-harness">Harnex</a> ·
  <a href="https://daniele21.github.io/">Mission</a>
</p>

<table>
<tr>
<td align="center">

### 🖼️ IMAGE PLACEHOLDER — HERO PRODUCT DEMO

**Target asset:** 20–30 second looping GIF or short video

**What it must show:**  
`Import PDF → Select what to protect → Local AI analysis → Review masked findings → Confirm redactions → Export protected PDF`

Use a real Android capture with large, readable UI, no debug overlays and only synthetic PII. This is the primary above-the-fold visual: a visitor should understand the product without reading the rest of the README.

</td>
</tr>
</table>

## Why RedactGuard

Sensitive documents often need to be processed **before** they can safely enter another workflow.

RedactGuard explores a concrete question:

> **Can useful AI processing happen locally, while keeping the original document under the user's control?**

It is an Android document-protection application and a real-world reference consumer of **Harnex**, the shared Local AI control plane and runtime layer.

RedactGuard deliberately separates the **product problem** from the **AI runtime problem**:

- **RedactGuard owns** document ingestion, PII policy, review, deterministic redaction and export.
- **Harnex owns** model/runtime administration, caller authorization, lifecycle, scheduling and model residency.
- **The user owns the final decision.** Findings are reviewed before redaction.
- **Failure is explicit.** Invalid AI output, unavailable Local AI or authorization problems do not trigger a hidden cloud fallback.

This makes RedactGuard more than an LLM demo: it is a reference implementation of a **privacy-sensitive Local AI product boundary on Android**.

## What it does

RedactGuard can:

- import a PDF or accept pasted text;
- let the user choose built-in or custom PII definitions;
- analyze content through a separately installed Harnex host;
- keep detected sensitive values masked by default in the review experience;
- let the user confirm or reject findings;
- validate structured AI output before it reaches the redaction workflow;
- export a new redacted PDF through Android's system file flow;
- surface privacy-safe diagnostics and recovery actions when Local AI is unavailable or fails.

### Product flow

```text
Import PDF / paste text
          │
          ▼
Choose what to protect
          │
          ▼
Run Local AI analysis
          │
          ▼
Validate structured findings
          │
          ▼
Human review
          │
          ▼
Deterministic redaction
          │
          ▼
Export minimized document
```

<table>
<tr>
<td align="center">

### 🖼️ IMAGE PLACEHOLDER — PRODUCT JOURNEY

**Target asset:** horizontal 5–6 frame product walkthrough

**What it must show:**  
`Import → Protection selection → Analysis → Review → Confirm → Export`

Use real RedactGuard screens and the same synthetic document throughout. Make the responsibility split visually obvious: **AI proposes findings; the user decides what is redacted**.

</td>
</tr>
</table>

## Why Local AI matters here

For this use case, privacy is not an abstract feature. The source material may contain names, contact details, financial information, health information, identifiers or other data that the user intends to remove **before sharing the document elsewhere**.

RedactGuard therefore follows four product rules:

| Principle | Behavior |
| --- | --- |
| **Local-first processing** | The source document is not silently sent to a cloud model. |
| **Human control** | AI findings are reviewable; detection is not the final redaction decision. |
| **Fail closed** | Missing runtime, authorization problems and invalid results do not silently bypass protection. |
| **Data minimization** | The intended external artifact is the protected document, not the original sensitive source. |

RedactGuard is **not a compliance guarantee** and automated detection can miss sensitive information. The goal is to reduce unnecessary exposure while keeping the decision boundary explicit.

## How it works

RedactGuard does **not** embed `llama.cpp`, GGUF files or model-management logic inside the application. It consumes the published Harnex Consumer Android SDK and reaches the Harnex host through Android Binder.

```text
┌────────────────────────── REDACTGUARD ──────────────────────────┐
│                                                                 │
│  Document ingestion → PII policy → AI request → Human review   │
│                                      │                          │
│                                      └→ deterministic redaction │
│                                                   → PDF export   │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                    Harnex Consumer Android SDK
                               │
                         Android Binder
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                              HARNEX                             │
│                                                                 │
│  Caller authorization → use-case policy → runtime scheduling   │
│           → model lifecycle / residency → local inference       │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                         Local AI model
```

<table>
<tr>
<td align="center">

### 🖼️ IMAGE PLACEHOLDER — ARCHITECTURE / WHO OWNS WHAT

**Target asset:** polished technical architecture diagram

**Required structure:**  
`RedactGuard → Consumer Android SDK → Binder boundary → Harnex Control Plane → Runtime orchestration → llama.cpp/backend → GGUF models`

RedactGuard zone: **document workflow, PII policy, strict result validation, human review, redaction/export**.  
Harnex zone: **authorization, control plane, scheduling, runtime lifecycle/residency, backend and models**.

**Core message:** the application owns the user problem; **Harnex owns reusable Local AI infrastructure**.

</td>
</tr>
</table>

This boundary is intentional. Product code must not depend on Harnex runtime internals, native pointers, private model paths or GGUF identity. See [`docs/architecture.md`](docs/architecture.md) for the full ownership model.

## Built to fail safely

A Local AI product is not production-like only when inference succeeds. It also needs explicit behavior when the surrounding system changes underneath it.

RedactGuard and Harnex are tested across failure and lifecycle conditions including:

- Harnex unavailable at startup;
- Consumer-first installation followed by Harnex installation;
- pending and denied authorization;
- independently signed Harnex and RedactGuard applications;
- Binder loss and reconnect;
- host process loss;
- RedactGuard process/activity recreation;
- explicit cancellation;
- critical memory-pressure scenarios;
- replacement-signer denial;
- malformed or invalid structured model results.

The repository contains dedicated emulator and cross-APK suites for these boundaries, rather than treating a successful single inference as sufficient evidence.

<table>
<tr>
<td align="center">

### 🖼️ IMAGE PLACEHOLDER — FAILURE & RECOVERY MATRIX

**Target asset:** compact engineering matrix

| Scenario | Expected product behavior |
| --- | --- |
| Harnex missing | Unavailable / fail closed |
| Authorization pending | Block until explicitly authorized |
| Signer changed | Deny |
| Binder lost | Reconnect or fail explicitly |
| Host process killed | Recover or fail explicitly |
| Invalid LLM output | Reject result |
| Critical memory pressure | Controlled failure |

The visual should communicate one idea: **failure semantics are designed, observable and tested — not accidental**.

</td>
</tr>
</table>

## Measured, not assumed

RedactGuard keeps model-quality acceptance separate from normal application correctness.

The versioned synthetic PII corpus and quality policy define explicit thresholds instead of relying on a handful of successful prompts:

| Metric | Acceptance threshold |
| --- | ---: |
| Aggregate precision | ≥ 0.90 |
| Aggregate recall | ≥ 0.98 |
| Aggregate F1 | ≥ 0.94 |
| Per-category precision | ≥ 0.80 |
| Per-category recall | ≥ 0.90 |
| Per-category F1 | ≥ 0.85 |
| Structured completion | ≥ 0.98 |
| Invalid finding rate | ≤ 0.02 |
| Invalid result rate | = 0.00 |

The corpus is versioned and frozen so a model or prompt cannot be made to "pass" by silently changing the benchmark after observing results. See [`docs/quality-policy-v1.md`](docs/quality-policy-v1.md).

<table>
<tr>
<td align="center">

### 🖼️ IMAGE PLACEHOLDER — QUALITY EVIDENCE

**Target asset:** benchmark/evidence card built from real representative runs

**Fields to show:**  
`Model · Quantization · Device · Corpus version · Precision · Recall · F1 · Structured completion · Invalid-result rate`

Clearly separate **automated/emulator evidence** from **representative physical-device inference evidence**. Do not publish illustrative numbers as if they were measured results.

</td>
</tr>
</table>

## Try it

### Requirements

For the full Local AI journey you need:

- an Android development environment compatible with the repository;
- a device or emulator for the RedactGuard application;
- a compatible **Harnex** host installed and configured on the same Android environment.

Harnex is a separate application because runtime/model ownership is intentionally shared infrastructure rather than bundled into every consumer.

### Build RedactGuard

```bash
git clone https://github.com/daniele21/redactguard-android.git
cd redactguard-android

./gradlew --version
bash scripts/doctor-android.sh
./gradlew :app:installDebug
```

For the complete Harnex + RedactGuard setup and two-APK evidence path, start with [`docs/evidence/physical-two-apk.md`](docs/evidence/physical-two-apk.md).

## RedactGuard + Harnex

RedactGuard is a **reference Harnex consumer**: a real application used to exercise the boundary between a product and reusable Local AI infrastructure.

That distinction matters because the long-term architecture is not:

```text
Every Android app
    └── bundles its own model + runtime + lifecycle + policy
```

It is closer to:

```text
RedactGuard ─┐
App B ───────┼──► Harnex ─► shared Local AI infrastructure
App C ───────┘
```

RedactGuard gives Harnex a privacy-sensitive workload with real document state, strict structured output, human review, cancellation, recovery and security boundaries. Harnex lets RedactGuard focus on the product instead of becoming another bespoke Android LLM runtime.

Learn more in the [Harnex repository](https://github.com/daniele21/android-local-llm-harness).

## Current status

The current `dev` candidate consumes Harnex Consumer Android SDK `0.1.0-alpha.11`, supports independently signed Harnex/RedactGuard installations and has been published to Google Play Internal Testing.

Automated API 35 integration covers the independent-signer and Two-APK lifecycle/fault paths, including Consumer-first installation, fail-closed pending authorization, exact application identity authorization, connect/disconnect/reconnect behavior, UI state continuity and replacement-signer denial.

### Current limits

- OCR and VLM-based document understanding are out of scope;
- there is no cloud parsing fallback;
- RedactGuard does not own model selection, GGUF files or Harnex administration;
- automated emulator evidence is not presented as proof of production ARM64/JNI/GGUF, memory, thermal or OEM behavior;
- representative physical-device and Play-signing evidence remains a separate release/evidence concern where required.

For exact integrated state and open evidence gates, see [`docs/current-state.md`](docs/current-state.md).

## Repository map

| If you want to understand… | Start here |
| --- | --- |
| Product and architecture | [`docs/architecture.md`](docs/architecture.md) |
| Exact current integrated state | [`docs/current-state.md`](docs/current-state.md) |
| Product features and contracts | [`docs/features/`](docs/features/) |
| Failure and recovery evidence | [`docs/evidence/failure-recovery-matrix.md`](docs/evidence/failure-recovery-matrix.md) |
| Harnex + RedactGuard device path | [`docs/evidence/physical-two-apk.md`](docs/evidence/physical-two-apk.md) |
| Model-quality acceptance policy | [`docs/quality-policy-v1.md`](docs/quality-policy-v1.md) |
| Play internal testing | [`docs/release/play-internal-testing.md`](docs/release/play-internal-testing.md) |
| Design system / UX contracts | [`design/`](design/) |
| Contribution workflow | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## Develop and validate

Contributors work from `dev` and follow [`AGENTS.md`](AGENTS.md). Canonical repository commands live in [`.engineering/commands.json`](.engineering/commands.json).

The normal development install is:

```bash
./gradlew :app:installDebug
```

Validation is selected by change blast radius. Cross-app Binder, privacy, packaging, security and release claims require stronger evidence than a normal UI or domain edit.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the contribution and validation model.

## Project context

RedactGuard is part of [Daniele Moltisanti's Local AI work](https://daniele21.github.io/): build real applications on top of reusable Local AI infrastructure, measure their behavior and limits, and use evidence to decide where **Local, Hybrid or Cloud** actually makes sense.

RedactGuard is the privacy-sensitive document use case in that broader system; **Harnex is the Local AI backbone**.
