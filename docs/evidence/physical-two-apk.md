# Physical two-APK evidence

Status: active — LAS-07 representative real-environment gate
Owner: RedactGuard + Harnex
Last reviewed: 2026-09-04

This gate closes the fidelity gap that CI and the API 35 x86_64 Two-APK emulator cannot establish. It must run on a representative physical Android `arm64-v8a` device with the production Harnex llama.cpp/JNI path and a real compatible GGUF.

Automated emulator evidence remains authoritative for Android/Binder/job semantics already proven there; physical evidence confirms only the dimensions that genuinely require representative hardware and operator judgement.

## Current LAS baseline

The current publication baseline is:

- integrated Harnex candidate: `dev@6b34fe9fcba70f6b8abd107fd58b61c418ac737d`;
- public Consumer SDK: `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.10`;
- Harnex phone-test from that integrated source: published successfully to Google Play Internal Testing;
- Harnex structured-output preset: `qwen35-json` revision `3` unless the exact candidate truthfully publishes a newer revision;
- previous complete RedactGuard automated baseline: exact-head preflight #946 and Two-APK #144 on RedactGuard `effd57f1723cffb56c45224a09e87d3f454f7827` against the pre-integration Harnex source;
- current RedactGuard alpha.10/integrated-Harnex convergence HEAD: must obtain fresh exact-head deterministic and Two-APK evidence before it becomes the physical candidate.

Immediately before a physical run, freeze the exact reviewed RedactGuard candidate that passed the fresh convergence matrix. If Harnex moves materially beyond `6b34fe9f...` or RedactGuard changes product/runtime/dependency behavior after that green matrix, re-establish automated evidence first.

The physical run must record the exact source revisions actually built and exercised. Do not substitute historical CRV/RG-HCP or pre-alpha.10 identities.

## Evidence set

LAS-07 is one gate composed of two complementary evidence sets on a representative ARM64 device.

### A. Harnex native runtime evidence

Use Harnex's canonical physical-device evidence runner from the exact integrated Harnex candidate:

```bash
git fetch origin
git switch --detach 6b34fe9fcba70f6b8abd107fd58b61c418ac737d
git status --porcelain
bash scripts/capture-device-e2e-evidence.sh \
  --model /absolute/path/to/model.gguf \
  --architecture qwen2 \
  --quantization Q4_K_M \
  --memory-repeat 5 \
  --max-pss-growth-kb 131072
```

Use a real compatible curated Qwen3.5 GGUF, preferably the same model that will be made available to the Host for the RedactGuard two-APK journey. The Harnex evidence bundle owns:

- `arm64-v8a` device identity;
- packaged production JNI/llama.cpp libraries;
- real GGUF identity and SHA-256;
- inspect/import/verify/load/generate/stream/release/unload/shutdown;
- active cancellation;
- repeated lifecycle/PSS evidence;
- thermal before/after evidence when exposed by the device;
- exact clean Harnex repository revision.

Review the bundle against Harnex `docs/device-e2e-evidence.md`. A green emulator or deterministic backend is not a substitute for this lane.

### B. RedactGuard + Harnex physical two-APK evidence

Build exact same-signer release APKs from clean checkouts. Do not assume Play-installed Harnex and RedactGuard satisfy the signature-protected Binder permission: Play App Signing identity must be verified separately. The canonical LAS-07 Binder proof uses repository-owned same-signer release APKs.

Harnex:

```bash
git fetch origin
git switch --detach 6b34fe9fcba70f6b8abd107fd58b61c418ac737d
git status --porcelain
bash scripts/build-phone-test-release.sh build-apk
```

Expected APK:

```text
apps/local-llm-phone-test/build/outputs/apk/release/local-llm-phone-test-release.apk
```

RedactGuard:

```bash
git fetch origin
git switch --detach <CURRENT_VALIDATED_REDACTGUARD_CANDIDATE>
git status --porcelain
bash scripts/build-redactguard-release.sh build-apk
```

Expected APK:

```text
app/build/outputs/apk/release/app-release.apk
```

Both helpers fail closed on dirty source and use the same existing Harness upload key by default. Signing material must remain outside the repositories and must never be captured in evidence.

Run the canonical interactive gate from the same exact RedactGuard checkout used to build its APK so `APP_SOURCE_REVISION` is truthful:

```bash
bash scripts/e2e-redactguard-device.sh \
  --device <SERIAL> \
  --host-apk <HARNESS_HOST_APK> \
  --app-apk <REDACTGUARD_APK> \
  --host-source-revision 6b34fe9fcba70f6b8abd107fd58b61c418ac737d \
  --preset-revision 3 \
  --release
```

Before attesting `HOST_READY`, confirm the actual Harnex Control Plane shows the RedactGuard PII assignment and source-backed `qwen35-json` preset revision. If the exact candidate truthfully shows a different published revision, stop and reconcile the candidate rather than forcing revision `3`.

The command intentionally remains interactive because SAF selection, background/app-switch observation, Review decisions, Host death/recovery, export inspection and final usability judgement require a real operator for this gate.

## What the physical two-APK runner guarantees

The command:

- refuses to replace pre-existing Harnex or RedactGuard packages;
- verifies both APKs have the same signer before installation;
- records APK SHA-256, signer, source revisions, Consumer SDK version, device/API/ABI and preset revision;
- stages RedactGuard first so truthful Host-absent behavior is observable;
- installs the exact Harnex APK and keeps setup/model ownership in Harnex;
- requires the real Consumer SDK/Binder path for product analysis;
- requires an explicit Android Home/return checkpoint during active real-Harnex-backed analysis;
- records operator attestations for input, background continuity, review, recovery, export and process-local persistence;
- removes only packages installed by the run and verifies cleanup;
- writes privacy-safe evidence under `evidence/local/e2e/` without document text, prompts, findings or raw Binder payloads.

`physical-two-apk-preflight.sh` is only a lower-level same-signer install/launch helper. It is not the canonical LAS-07 gate.

## Required scenario matrix

Use synthetic fixtures only. Private, production or client documents must not be committed, attached to evidence or copied into logs.

### Input and parsing

1. **Pasted text:** synthetic text containing a full name, email, phone number and postal address reaches Definitions and the canonical analysis path without invoking the PDF parser.
2. **Single-page text PDF:** extraction succeeds and reaches Definitions/analysis without `RG-PDF-004` or `RG-PDF-005`.
3. **Multi-page text PDF:** page order/canonical segmentation remain stable and no truncation is silently accepted.
4. **Image-only PDF:** fails explicitly as `RG-PDF-008 / IMAGE_ONLY_PDF`; OCR/VLM is not invoked automatically.
5. **Unexpected parser failure, if reproduced:** surfaces `RG-PDF-005 / PARSER_FAILED`, not `MALFORMED_PDF`; technical details remain privacy-safe.

### Local AI, native runtime and lifecycle

1. Start with RedactGuard installed and Harnex absent. Confirm Local AI is unavailable/not installed and analysis cannot start.
2. Install/start the exact Harnex release APK, complete Harnex-owned assignment/preset/model setup, return to RedactGuard and confirm connectivity recovers without reinstalling RedactGuard.
3. Start analysis and verify the authoritative Control Plane path resolves the assigned preset and prepares/generates through Harnex without a manual consumer-side model-load action.
4. Confirm the successful product analysis is using the same representative device/model family already covered by the Harnex native evidence bundle. RedactGuard must not expose model/runtime tuning controls.
5. During active analysis, send RedactGuard to Android Home and return. The accepted analysis remains authoritative or completes normally; ordinary UI detachment must not implicitly cancel or duplicate inference/result.
6. Multi-chunk analysis completes sequentially when required. A later chunk failure exposes no partial findings.
7. Cancellation during active generation terminates safely and exposes no partial review result.
8. Harnex process death/restart during analysis produces classified interruption/recovery rather than silently claiming native continuation or reusing stale execution identity.
9. Exercise a stale/withdrawn preset or missing-binding path where practical and verify fail-closed configuration/readiness rather than a phone-global model fallback.

### Review and export

1. Findings are hidden by default. Explicitly reveal then hide at least one synthetic value.
2. Mark at least one occurrence `Oscura` and another `Ignora`; export remains unavailable until required decisions are complete.
3. Export to a new PDF and reopen it independently. Accepted synthetic PII is absent/replaced and ignored text remains present.
4. Exercise an unwritable/failed destination where practical. Partial output is cleaned best-effort and success is never reported falsely.

### Process-local privacy and cleanup

1. Starting a new document clears prior task-local input/findings/reveal/review state.
2. Kill and relaunch RedactGuard; sensitive task state is not resurrected from persistent state.
3. Diagnostic/evidence output contains stable codes/build/run/environment identity but no prompts, document text, finding values, model paths, raw model output or raw Binder payloads.
4. The E2E command removes only packages it installed and verifies the target returns to the initial package-absence state.

## LAS-07 pass criteria

LAS-07 passes only when both evidence sets are reviewed together and support one coherent representative-device claim:

1. Harnex native evidence is green on a physical `arm64-v8a` device with a real compatible GGUF and exact Harnex `6b34fe9f...` source identity.
2. The Harnex bundle contains successful generation/cancellation and repeated lifecycle evidence, expected native library inventory, bounded PSS behavior and no native crash/unrecoverable runtime state.
3. RedactGuard two-APK evidence is green using exact same-signer Harnex + RedactGuard release APKs and the real Consumer SDK/Binder path with Consumer SDK `0.1.0-alpha.10`.
4. Background Home/return continuity is explicitly attested while real Harnex-backed work is active.
5. Host death is treated as a truthful interruption boundary; no claim is made that native state survives Harnex process death.
6. Privacy, Review/export and package cleanup checkpoints pass using synthetic data only.
7. Physical thermal/resource observations are recorded without generalizing one device to every OEM/device combination.

A single representative device closes the acceptance path for that matrix entry. It does not prove universal OEM compatibility or justify broad performance claims by itself.

## Evidence identity to retain

Retain or attach privacy-safe summaries containing:

- exact Harnex and RedactGuard source revisions;
- both release APK SHA-256 values and common signer SHA-256;
- Consumer SDK version;
- Harnex preset revision;
- device manufacturer/model, Android release/API and ABI;
- GGUF filename/architecture/quantization/byte size/SHA-256 from the Harnex native evidence bundle;
- native generation/cancellation/PSS/thermal markers;
- RedactGuard physical E2E `result.json` with operator attestations and verified cleanup.

Do not retain signing secrets, GGUF bytes, prompts, document text, findings, raw output or private client data.
