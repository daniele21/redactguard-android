# Failure diagnostics hardening

Status: active
Owner: redactguard-android
Read when: completing representative failure/recovery evidence and finalizing the product failure contract

## Goal

Prove on representative Android/device boundaries that RedactGuard preserves stable, actionable failure identity across import, local-AI analysis, review and export without leaking document content, then transfer the remaining durable rules and close this workstream.

## Non-goals

- OCR/VLM for image-only PDFs;
- raw stack traces or a developer console in normal product UI;
- cloud telemetry or content upload;
- changing Harness runtime/model ownership.

## Invariants

- classify a failure at the boundary that can truthfully identify it;
- preserve known cause identity through adapters/domain/ViewModel/UI;
- stable product codes do not depend on exception-class names;
- user copy states what failed and the correct recovery when known;
- technical code/stage/operation ID are progressively disclosed;
- document text, prompts, findings, raw Binder/model payloads and sensitive filenames stay out of normal diagnostics;
- user cancellation is a lifecycle outcome, not an unexplained failure;
- analysis remains atomic: failed multi-chunk analysis never exposes partial findings as valid review output;
- export failures cannot leave a valid-looking partial result.

## Integrated repository behavior

The repository implementation already contains:

- distinct stable PDF/import causes including source missing/unreadable, encrypted, malformed, parser failure, explicit limits, empty and image-only (`RG-PDF-008`);
- product-owned local-AI/analysis failure projection that preserves distinguishable Host/permission/protocol/result/cancellation causes where the lower layer provides them;
- `RG-AI-012 LOCAL_AI_INTERNAL` for unexpected unchecked failures at the Control Plane / Consumer SDK boundary, with only privacy-safe step and whitelisted type metadata preserved;
- review/export failure classification and cause-specific recovery;
- privacy-safe structured diagnostic context with operation identity;
- progressive error UI with user explanation/recovery first and technical details secondary;
- mapping/privacy/lifecycle tests and failure-contract guardrails.

The current diagnostics candidate additionally preserves typed Harness identities on both local-AI boundaries. Control Plane rejections are retained as `ControlPlane:<ENUM>` and Consumer failures are retained as `Consumer:<ENUM>` together with the exact safe boundary step (`consumer.prepare`, `consumer.create-session` or `consumer.generate`). Stable product families do not change: for example, connected `RUNTIME_FAILURE`, `PREPARE_FAILED` and `SESSION_NOT_FOUND` still produce `GENERATION_FAILED` and ultimately `RG-AI-008 CHUNK_FAILED`. Free-form `ConsumerFailure.message`, Binder detail, document text and model payloads are discarded rather than propagated.

Repository validation for the current candidate must be green before merge. That evidence does not replace the remaining physical gate.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| FD-1 | Canonical product failure registry/contract | domain failure owners/tests | — | yes | DONE |
| FD-2 | Preserve PDF/import failures | extraction mapping/UI/tests | FD-1 | yes | DONE |
| FD-3 | Preserve local-AI/analysis failures | runtime/analysis mapping/tests | FD-1 | yes | ACTIVE |
| FD-4 | Review/export recovery | review/export mapping/tests | FD-1 | yes | DONE |
| FD-5 | Privacy-safe diagnostics/operation identity | diagnostics + privacy tests | FD-1 | yes | ACTIVE |
| FD-6 | Progressive error UI | product error projection/screens | FD-2..FD-5 | no | DONE |
| FD-7 | Contract/lifecycle guardrails | tests/static failure checks | FD-2..FD-6 | yes | ACTIVE |
| FD-8 | Representative physical failure/recovery evidence | device evidence only | FD-2..FD-7 | no | ACTIVE |
| FD-9 | Durable handoff and delete plan | canonical docs/current state | FD-8 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Current executable slice — FD-3 / FD-5 -> FD-8

Earlier representative release installed-pair evidence with Harness `versionCode=35` and RedactGuard `versionCode=12` proved that both APKs share the same signer, RedactGuard has the signature-gated `USE_LOCAL_LLM` permission, `HarnessSharedRuntimeService` is live in ActivityManager and RedactGuard is present in its service connection state. At that point the reproduced product failure was `RG-AI-002 / HOST_UNAVAILABLE`, which narrowed the investigation away from package/signature/basic Binder availability and toward the local-AI control/runtime path.

The latest reported physical reproduction now advances into analysis and surfaces `RG-AI-008 / CHUNK_FAILED`. That is progress in the path but not yet root-cause evidence: before this candidate, `ConsumerAnalysisRuntime` intentionally collapsed connected `RUNTIME_FAILURE`, `PREPARE_FAILED` and `SESSION_NOT_FOUND` into the same `GENERATION_FAILED` family and discarded the original typed `ConsumerErrorCode`.

The current candidate closes both observability gaps without broadening the data boundary:

1. Binder connection snapshots emit only state plus a whitelist-derived detail identity;
2. assignment discovery, published-preset validation, activation and deactivation emit bounded `RG_LOCAL_AI` step/result/reason events;
3. typed Control Plane rejections preserve their original enum identity in `AnalysisRuntimeDiagnostic` while retaining the existing stable product failure family;
4. typed Consumer rejections/failures preserve the exact safe boundary step plus `Consumer:<ENUM>` while retaining the existing stable product failure family;
5. `ConsumerFailure.message` is never copied into the app-owned runtime exception, product failure, UI model or diagnostic store;
6. exhaustive failure-matrix tests cover every `ConsumerErrorCode` for synchronous generation rejection and asynchronous generation failure with connected/disconnected transport, while focused tests verify prepare/session/generate diagnostic steps;
7. `scripts/diagnose-redactguard-local-ai-device.sh` diagnoses an already-installed pair without install/uninstall/`pm clear`, trusts live ActivityManager evidence over brittle package-dump string matching and captures only the dedicated safe tag;
8. the diagnostic runner classifies the last Control Plane rejection into `diagnosis.txt` so representative runs can distinguish configuration, model availability, conflict and activation-lifecycle ownership directly.

After automated STRONG validation, install the exact same-signer candidate without clearing Harness data/model state and reproduce the current analysis failure. The progressively disclosed technical details must identify the stable product code plus the safe Consumer boundary identity. Use that evidence to determine whether the owning functional defect is preparation, session lifecycle or generation/runtime before changing Harness or RedactGuard behavior.

Record exact RedactGuard APK/build/source identity, Harness APK/build/source identity and device/API identity for representative scenarios:

1. Host absent -> actionable local-AI unavailable state -> recovery after Host becomes available;
2. image-only PDF -> explicit `RG-PDF-008` and no OCR fallback;
3. at least one other classified import failure such as encrypted/corrupt/unreadable input where a safe fixture is practical;
4. user cancellation during analysis -> cleanup -> recoverable state;
5. Host death/disconnect during analysis -> classified failure -> reconnect/retry;
6. export destination/write failure -> no valid-looking partial output -> retry with the correct recovery action;
7. process recreation/error recovery does not resurrect sensitive document state;
8. captured diagnostic evidence contains stable identity/code/stage but no sensitive document content;
9. unexpected Local AI boundary failure, if still reproducible, exposes `RG-AI-012` with a safe step/type and no exception message or user/model content;
10. typed Control Plane rejection, if reproduced, is visible in bounded technical evidence without exposing model/document payload content;
11. typed Consumer failure, if `RG-AI-008` reproduces, exposes `consumer.prepare`, `consumer.create-session` or `consumer.generate` plus `Consumer:<ENUM>` without exposing `ConsumerFailure.message`.

Real user documents containing PII must not be committed as fixtures or evidence.

## Validation

Repository side remains covered by the canonical `check`, `test` and build gates plus failure mapping/privacy/lifecycle tests. Because the current candidate changes the Harness Consumer adapter and a privacy-safe diagnostic boundary, validation depth is STRONG even though the public Harness protocol is unchanged. The installed-device diagnostic script also requires shell syntax validation. FD-8 requires the physical two-APK runbook or the non-destructive installed-pair diagnostic for focused root-cause evidence; emulator/CI evidence must be labelled as such and cannot complete the representative physical workstream.

## Durable destinations

- `docs/architecture.md` — failure ownership/privacy boundaries only if not already represented;
- `docs/features/local-ai-consumer.md` / `docs/features/local-ai-runtime-adapter.md` — current Control Plane and diagnostic semantics;
- tests/diagnostic contracts — executable stable mapping/privacy truth;
- `design/ux-contract.json` — user-facing error/recovery hierarchy only if product copy/hierarchy changes.

## Completion

After FD-8 is green, transfer only missing durable current behavior, update `docs/current-state.md`, remove links to this workstream and delete this file by default. Git retains the implementation history.
