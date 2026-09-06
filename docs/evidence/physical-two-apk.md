# Physical two-APK evidence

Status: active — focused Play signer/install-order release gate + LAS-07 representative real-environment gate
Owner: RedactGuard + Harnex
Last reviewed: 2026-09-06

This document owns physical evidence that API 35 emulator CI cannot establish. The production topology is **independently signed Harnex and RedactGuard applications**. Do not restore the superseded same-signer/custom-bind-permission assumption.

Automated independent-signer and Two-APK emulator evidence remains authoritative for the Binder/Control Plane/lifecycle semantics it directly proves. Physical evidence confirms only the dimensions that genuinely require Play App Signing, representative hardware or operator judgement.

## Current release topology

The current release candidate uses:

- Harnex Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`;
- a public Harnex Service that is explicitly bindable without a custom bind permission;
- Harnex-owned authorization derived from Binder UID -> exact installed package -> current signer -> persisted Control Plane authorization -> enabled use case;
- RedactGuard installed independently from Harnex and allowed to exist before the Host;
- source-observed `PENDING` as the fail-closed pre-authorization state;
- explicit Harnex authorization of the observed package/signer identity;
- Settings-owned RedactGuard Connect / Disconnect / Reconnect using Consumer SDK `disconnect()`;
- fail-closed signer replacement surfaced by Harnex as `SIGNATURE_CHANGED`.

Both current candidates are published to Google Play Internal Testing. The exact Play-distributed version/source identities used for a physical run must be recorded at run time; do not substitute historical alpha.10 or same-signer identities.

## Evidence set A — focused Play signer/install-order confirmation

This is the release-critical confirmation for the independent-signing change. Use the actual Google Play Internal Testing distributions and a physical Android device enrolled as an internal tester.

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
7. **Representative analysis.** With a compatible real Harnex model configured, run a synthetic RedactGuard analysis through the production Binder/runtime path and complete masked review/redaction/export.
8. **Background continuity.** During active work, send RedactGuard to Android Home and return. Confirm no fake completion, duplicate analysis or implicit cancellation from ordinary UI detachment.
9. **Host interruption.** Where practical, exercise a Harnex process interruption/restart and confirm classified interruption/recovery rather than silent native-state continuation.
10. **Signer evidence.** Retain the actual Harnex and RedactGuard Play signing certificate SHA-256 values and confirm they are independently distributed identities. If a signer changes in a controlled replacement scenario, Harnex must fail closed rather than silently inheriting prior authorization.

### Pass criteria

The focused Play gate passes when the recorded evidence shows:

- RedactGuard was usable in a truthful Host-absent state before Harnex installation;
- later Harnex installation required no RedactGuard reinstall;
- the exact Play-installed RedactGuard identity appeared `PENDING` before authorization;
- Harnex authorization targeted the currently installed package/signer identity;
- Connect / Disconnect / Reconnect worked on the actual Play-installed pair;
- representative analysis used the production Consumer SDK/Binder path;
- no same-signer/custom-bind-permission assumption was required;
- privacy-safe identity/evidence metadata was retained.

Successful GitHub Actions publication or emulator E2E is not a substitute for this gate because neither proves the certificates Android observes for the Play-installed applications.

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
- both actual Play App Signing certificate SHA-256 values for the focused Play gate;
- Consumer SDK version (`0.1.0-alpha.11` for this candidate);
- Harnex use-case/preset revision used;
- device manufacturer/model, Android release/API and ABI;
- for LAS-07, GGUF identity plus native generation/cancellation/PSS/thermal markers;
- operator attestations for Consumer-first install, PENDING/authorization, Connect/Disconnect/Reconnect, representative analysis, background continuity, review/export and cleanup.

Do not retain signing secrets, GGUF bytes, prompts, document text, finding values, raw model output or private client data.

## Release interpretation

The focused Play signer/install-order gate closes the distribution-identity risk introduced by independently signed applications. LAS-07 closes broader native/runtime/device fidelity claims. Passing one does not silently pass the other.
