# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-08-24

## Integrated product state

RedactGuard is a standalone Android document-protection product. It consumes the published Harness Consumer Android SDK over Binder while model/runtime/GGUF/residency ownership remains in Harness.

The integrated product now includes:

- PDF and pasted-text ingestion converging on canonical `DocumentSegment` analysis input; image-only PDFs fail explicitly and OCR/VLM remains out of scope;
- product-owned PII definitions and process-local selection;
- bounded structured local analysis with strict result validation and atomic no-partial-findings semantics;
- stable product failure codes with actionable recovery and progressively disclosed privacy-safe diagnostics;
- hidden-by-default finding review, explicit reveal, deterministic redact/ignore decisions and fail-closed export eligibility;
- deterministic SAF PDF export with partial-output cleanup on failure;
- process-local sensitive document text, findings, reveal state and review decisions; no silent cloud fallback;
- task-first local-AI language rather than normal-surface Harness/Binder implementation vocabulary;
- semantic RedactGuard theme/tokens, compact/medium/expanded adaptive layout behavior and accessibility semantics;
- native Compose product-experience instrumentation tests that compile/package in CI;
- executable zero-residue Android smoke and guided physical two-APK E2E helpers;
- build identity distinct from product version, source revision/dirty identity, immutable promoted artifacts, manifest/SHA-256/build delta and bounded retention.

## Engineering baseline

The convergence branch adopts `repo-template-sw` 0.5.0 at L1 with `android` and `product-ui` profiles.

It includes:

- `.engineering/baseline.json`, `.engineering/commands.json` and documentation/repository policy;
- local structured-change, validation, workstream-finalization and product-experience skills;
- repository, operations, documentation, agent-context and product-experience verifiers;
- `Repository health` CI and PR evidence template;
- desired-state branch governance with a machine-verifiable policy/runbook.

`Repository health` has passed repository structure, operating contract, product-experience contract, documentation lifecycle and instruction-context verification on the converged RTA-1 base. Final exact-head `Validate` + `Repository health` after the current ledger/state update remain the merge gate.

Active alignment workstream:

`docs/workstreams/repo-template-sw-alignment.md`

## Remaining real-environment evidence

Repository implementation is ahead of external evidence in three bounded areas.

1. Product experience: run native instrumentation on an explicit Android target and record representative TalkBack, large-text and compact/medium/expanded physical-device checks using synthetic data only.
2. Two-APK integration: execute the same-signer Harness + RedactGuard physical flow covering pasted text/text PDF, local analysis, review, cancellation/recovery, Host absence/death/reconnect, export, independent reopen and cleanup.
3. GitHub governance: apply and then verify the documented live branch/default-branch/required-check protection policy. The repository currently owns and verifies desired state; it must not be reported as live enforcement until GitHub settings are actually changed.

Until those gates are recorded, do not claim physical-device completeness or live branch-governance enforcement solely from green CI.

Relevant active product workstreams remain:

- `docs/workstreams/document-ingestion-v2.md` — implementation green; physical ingestion evidence remains;
- `docs/workstreams/failure-diagnostics-hardening.md` — repository failure contract implemented; representative physical failure/recovery evidence remains;
- `docs/workstreams/ombra-to-redactguard-migration.md` — repository extraction complete; final physical cutover and Harness cleanup remain;
- `docs/workstreams/harness-control-plane-consumer-cutover.md` — multi-preset tolerance integrated; assigned-use-case/activation lifecycle remains dependent on corresponding Harness SDK/control-plane work.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harness administration to RedactGuard as part of alignment work. Those require separate owning capabilities/workstreams.
