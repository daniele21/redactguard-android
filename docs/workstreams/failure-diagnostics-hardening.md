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
- review/export failure classification and cause-specific recovery;
- privacy-safe structured diagnostic context with operation identity;
- progressive error UI with user explanation/recovery first and technical details secondary;
- mapping/privacy/lifecycle tests and failure-contract guardrails.

Repository validation for the integrated implementation is green. That evidence does not replace the remaining physical gate.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| FD-1 | Canonical product failure registry/contract | domain failure owners/tests | — | yes | DONE |
| FD-2 | Preserve PDF/import failures | extraction mapping/UI/tests | FD-1 | yes | DONE |
| FD-3 | Preserve local-AI/analysis failures | runtime/analysis mapping/tests | FD-1 | yes | DONE |
| FD-4 | Review/export recovery | review/export mapping/tests | FD-1 | yes | DONE |
| FD-5 | Privacy-safe diagnostics/operation identity | diagnostics + privacy tests | FD-1 | yes | DONE |
| FD-6 | Progressive error UI | product error projection/screens | FD-2..FD-5 | no | DONE |
| FD-7 | Contract/lifecycle guardrails | tests/static failure checks | FD-2..FD-6 | yes | DONE |
| FD-8 | Representative physical failure/recovery evidence | device evidence only | FD-2..FD-7 | no | ACTIVE |
| FD-9 | Durable handoff and delete plan | canonical docs/current state | FD-8 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Current executable slice — FD-8

Record exact RedactGuard APK/build/source identity, Harness APK/build/source identity and device/API identity for representative scenarios:

1. Host absent -> actionable local-AI unavailable state -> recovery after Host becomes available;
2. image-only PDF -> explicit `RG-PDF-008` and no OCR fallback;
3. at least one other classified import failure such as encrypted/corrupt/unreadable input where a safe fixture is practical;
4. user cancellation during analysis -> cleanup -> recoverable state;
5. Host death/disconnect during analysis -> classified failure -> reconnect/retry;
6. export destination/write failure -> no valid-looking partial output -> retry with the correct recovery action;
7. process recreation/error recovery does not resurrect sensitive document state;
8. captured diagnostic evidence contains stable identity/code/stage but no sensitive document content.

Real user documents containing PII must not be committed as fixtures or evidence.

## Validation

Repository side remains covered by the canonical `check`, `test` and build gates plus failure mapping/privacy/lifecycle tests. FD-8 requires the physical two-APK runbook; emulator/CI evidence must be labelled as such and cannot complete this workstream.

## Durable destinations

- `docs/architecture.md` — failure ownership/privacy boundaries only if not already represented;
- `docs/features/product-flow.md` / relevant feature docs — current recovery semantics;
- tests/diagnostic contracts — executable stable mapping/privacy truth;
- `design/ux-contract.json` — user-facing error/recovery hierarchy.

## Completion

After FD-8 is green, transfer only missing durable current behavior, update `docs/current-state.md`, remove links to this workstream and delete this file by default. Git retains the implementation history.
