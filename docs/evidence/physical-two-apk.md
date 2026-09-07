# Physical two-APK evidence

Status: active — pre-release Play install-order evidence + public-release signer qualification + LAS-07 representative real-environment gate
Owner: RedactGuard + Harnex
Last reviewed: 2026-09-07

This document owns physical evidence that API 35 emulator CI cannot establish. Harnex supports consumer applications with signer identities independent from the Host; however, a particular first-party pre-release pair may temporarily share a Play signing identity. Do not restore the superseded assumption that shared signing grants Binder authority.

Automated distinct-signer and Two-APK emulator evidence remains authoritative for the Binder/Control Plane/lifecycle semantics it directly proves. Physical evidence confirms only the dimensions that genuinely require Play App Signing, representative hardware or operator judgement.

## Current release topology

The current candidate uses:

- Harnex Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`;
- a public Harnex Service that is explicitly bindable without a custom bind permission;
- Harnex-owned authorization derived from Binder UID -> exact installed package -> current signer -> persisted Control Plane authorization -> enabled use case;
- RedactGuard installed independently from Harnex and allowed to exist before the Host;
- source-observed `PENDING` as the fail-closed pre-authorization state;
- explicit Harnex authorization of the observed package/signer identity;
- Settings-owned RedactGuard Connect / Disconnect / Reconnect using Consumer SDK `disconnect()`;
- fail-closed signer replacement surfaced by Harnex as `SIGNATURE_CHANGED`.

Both current candidates are published to Google Play Internal Testing. The exact Play-distributed version/source identities and signing identities used for a physical run must be recorded at run time.

The current pre-release Internal Testing pair has been observed with the same Play App Signing SHA-256 digest. That is a truthful property of the current first-party distribution, not the Binder authorization mechanism and not physical evidence of distinct-signer Play readiness.

## Evidence set A — focused Play install-order/authorization confirmation

This is the release-critical physical confirmation for the **current pre-release first-party topology**. Use the actual Google Play Internal Testing distributions and a physical Android device enrolled as an internal tester.

### Preconditions

- use a physical Android device with the tester account enabled for both apps;
- start from a known package state and record whether either app was already installed;
- record Android version/API, device model and ABI;
- record Harnex and RedactGuard package version/versionCode shown by the device/Play installation;
- record each installed package's signing certificate SHA-256 using a trustworthy local Android/package inspection method;
- never capture signing secrets, prompts, document contents, finding values or raw model output.

### Required journey

1. **Consumer first.** Install/update RedactGuard from Play Internal while Harnex is absent. Open RedactGuard and verify Local AI is unavailable/fail-closed; analysis must not claim connectivity.
2. **Host later.** Install/update Harnex from Play Internal without reinstalling RedactGuard.
3. **Observed identity.** Open Harnex and confirm Applications/Control Plane observes the RedactGuard package and its actual current signer. The state must be `PENDING` before explicit approval.
4. **Explicit authorization.** Authorize the exact observed RedactGuard identity in Harnex for the intended use case. Do not authorize by a manually entered or stale signer alias.
5. **Connect.** Return to RedactGuard and confirm the connection recovers without reinstalling the app.
6. **Disconnect / Reconnect.** In RedactGuard Settings, Disconnect; verify the disconnected preference is real and persists across ordinary lifecycle movement. Reconnect and confirm a fresh connection epoch succeeds.
7. **Representative analysis.** With a compatible real Harnex model configured, run a synthetic RedactGuard analysis through the production Binder/runtime path and complete masked review/redaction/export where practical for the focused run.
8. **Background continuity.** During active work, send RedactGuard to Android Home and return. Confirm no fake completion, duplicate analysis or implicit cancellation from ordinary UI detachment where this dimension is in scope.
9. **Host interruption.** Where practical, exercise a Harnex process interruption/restart and confirm classified interruption/recovery rather than silent native-state continuation.
10. **Signer evidence.** Retain the actual Harnex and RedactGuard Play signing certificate SHA-256 values. Record whether the pair is same-signer or distinct-signer; do not infer one from package separation.

### Pass criteria for pre-release stable repository promotion

The focused Play gate passes for a pre-release `dev -> main` promotion when the recorded evidence shows:

- RedactGuard was usable in a truthful Host-absent state before Harnex installation;
- later Harnex installation required no RedactGuard reinstall;
- the exact Play-installed RedactGuard identity appeared `PENDING` before authorization;
- Harnex authorization targeted the currently installed package/signer identity;
- Connect / Disconnect / Reconnect worked on the actual Play-installed pair;
- representative analysis used the production Consumer SDK/Binder path;
- the actual Play signer topology was recorded truthfully;
- no signing assumption was used as the Binder authorization decision;
- privacy-safe identity/evidence metadata was retained.

For this pre-release milestone, the first-party pair may be same-signer. This passes only the current topology/install-order/runtime claim; it does **not** qualify physical distinct-signer Play distribution.

Successful GitHub Actions publication or emulator E2E is not a substitute for this focused physical gate because neither proves the certificates Android observes for the Play-installed applications or the real install-order recovery journey.

## Evidence set A2 — public distinct-signer Play qualification

Before the first public release that depends on independently signed Harnex/Consumer distribution, or before claiming physical distinct-signer Play readiness, repeat the focused journey with actual Play-installed Harnex and Consumer applications whose observed signing certificate SHA-256 values are different.

The A2 gate passes only when:

- the actual Play-installed Host and Consumer certificate digests are distinct;
- the Consumer is still `PENDING` before explicit Harnex authorization;
- authorization targets the exact observed distinct-signer identity;
- Connect / Disconnect / Reconnect and representative production Binder/runtime use succeed;
- a controlled signer-replacement scenario, where practical, fails closed rather than inheriting prior authorization.

A deterministic distinct-signer E2E remains required regardless of whether the first-party apps currently share a signer. A2 is additional distribution-fidelity evidence, not a replacement for deterministic authorization tests.

## Evidence set B — LAS-07 representative ARM64/runtime evidence

The broader LAS-07 gate remains separate. It requires a representative physical Android `arm64-v8a` device, the production Harnex llama.cpp/JNI path and a real compatible GGUF.

Use Harnex's canonical physical-device runner from the exact Harnex source identity under qualification:

```bash
git fetch origin
git switch --detach <EXACT_HARNEX_SOURCE_REVISION>
git status --porcelain
bash scripts/capture-device-e2e-evidence.sh \
  --model /absolute/path/to/model.gguf \
  --architecture qwen2 \
  --quantization Q4_K_M \
  --memory-repeat 5 \
  --max-pss-growth-kb 131072
```

Use a real compatible curated Qwen3.5 GGUF, preferably the same model family available to Harnex during the focused RedactGuard product journey. Review the bundle against Harnex `docs/device-e2e-evidence.md`.

The Harnex native bundle owns:

- physical `arm64-v8a` device identity;
- packaged production JNI/llama.cpp library identity;
- GGUF filename/architecture/quantization/size/SHA-256;
- inspect/import/verify/load/generate/stream/cancel/release/unload/shutdown behavior;
- repeated lifecycle/PSS observations;
- thermal observations when exposed by the device;
- exact clean Harnex repository/source identity.

## Representative RedactGuard product matrix

Use synthetic fixtures only.

### Input and parsing

1. Pasted synthetic text with representative PII reaches Definitions and analysis without invoking PDF parsing.
2. Single-page and multi-page text PDFs preserve expected extraction/order.
3. Image-only PDFs fail explicitly; OCR/VLM is not invoked automatically.
4. Unexpected parser failures expose typed privacy-safe errors rather than fabricated success.

### Local AI and lifecycle

1. Host absence remains fail-closed.
2. Host-later installation and exact identity authorization recover without RedactGuard reinstall.
3. Analysis resolves through Harnex-owned assignment/preset/model/runtime policy.
4. Multi-chunk work remains bounded/sequential where required and exposes no partial findings on later failure.
5. Cancellation exposes no partial review result.
6. Harnex process loss/restart is a truthful interruption/recovery boundary.
7. Missing/stale assignment or preset stays fail-closed rather than selecting a phone-global fallback.

### Review and export

1. Findings are hidden by default; explicit reveal/hide works.
2. At least one synthetic occurrence is accepted for redaction and another ignored.
3. Exported PDF is reopened independently; accepted synthetic PII is redacted and ignored text remains.
4. Failed/unwritable output never reports false success and cleans partial output best-effort.

### Process-local privacy

1. Starting a new document clears prior task-local sensitive state.
2. Process death/relaunch does not resurrect sensitive document/prompt/finding/output content from persistent state.
3. Diagnostics/evidence contains stable identity/codes but no raw sensitive content or Binder payloads.

## Evidence identity to retain

Retain privacy-safe summaries containing:

- exact Harnex and RedactGuard source/build identities represented by the tested distributions;
- installed Harnex and RedactGuard version/versionCode;
- both actual Play App Signing certificate SHA-256 values and whether the run is same-signer pre-release evidence or distinct-signer public-release qualification;
- Consumer SDK version (`0.1.0-alpha.11` for this candidate);
- Harnex use-case/preset revision used;
- device manufacturer/model, Android release/API and ABI;
- for LAS-07, GGUF identity plus native generation/cancellation/PSS/thermal markers;
- operator attestations for Consumer-first install, PENDING/authorization, Connect/Disconnect/Reconnect, representative analysis, background continuity, review/export and cleanup as applicable to the run scope.

Do not retain signing secrets, GGUF bytes, prompts, document text, finding values, raw model output or private client data.

## Release interpretation

Evidence set A closes the current pre-release distribution/install-order risk for stable repository promotion when the actual signer topology is recorded truthfully. A same-signer first-party Internal Testing pair is acceptable for that pre-release milestone because signing is not the Binder authorization mechanism and deterministic distinct-signer E2E remains mandatory.

Evidence set A2 becomes blocking before the first public release that depends on independently signed application distribution, or before any claim of physical distinct-signer Play qualification.

LAS-07 closes broader native/runtime/device fidelity claims. Passing A, A2 or LAS-07 never silently passes the others.
