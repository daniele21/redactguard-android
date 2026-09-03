# RedactGuard — Coding Agent Guide

RedactGuard is a privacy-first Android document-protection app using local analysis and an optional Local AI Harness boundary. This guide owns routing and durable invariants, not project history.

## Read only what the task requires

Always read this file, then only the closest scoped guide, owning code/contracts/tests and relevant canonical docs. Read:

- `.engineering/commands.json` for delivery stage, validation/execution/build routing;
- `.engineering/e2e.json` for complete-workflow/environment claims;
- `skills/validate-change/SKILL.md` during implementation;
- `skills/preflight-change/SKILL.md` when a coherent outcome becomes integration/release-ready;
- `skills/remote-preflight/SKILL.md` only for required deterministic gates unavailable locally;
- `design/*` + `design-product-experience` for meaningful UI changes.

Do not load all LAS workstreams or run release-grade Android validation for every edit.

## Durable invariants

- Sensitive document content remains local by default; no silent cloud fallback or sensitive-value logging.
- Redaction/privacy policy belongs to its domain owner, not UI/adapters.
- Harness/Consumer/Binder semantics remain explicit and truthful; Host absence/restart must not silently become a fake success path.
- Persisted/sensitive state has explicit lifecycle and cleanup semantics.
- UI exposes actionable user states without leaking unnecessary sensitive data and follows hierarchy/progressive disclosure/accessibility/adaptive behavior.
- Emulator evidence never becomes production ARM64/model/physical-device evidence by implication.
- Build/package identity and release behavior remain reproducible and privacy-safe.

## Ownership routing

Start with the canonical owner and inspect direct consumers/tests before shared changes. In particular:

- domain/privacy/redaction owners define document-protection policy;
- infrastructure owns persistence/platform/Harness adapters;
- UI/ViewModel layers project domain capability into user tasks and recovery states;
- Harness/Consumer/Binder integration requires contract and cross-process evidence;
- `design/*` owns adopted product-experience/brand routing.

## Delivery model

Delivery stage and validation depth are independent.

### ITERATION — default

Use while implementation is changing, including draft collaboration PRs.

Goal: fast falsification. Prefer Spotless/touched formatting, affected debug compile and focused unit/component/contract tests. Lint + debug APK + AndroidTest APK + R8 + E2E + exact-head preflight are not the default edit loop.

### INTEGRATION

Use when a coherent **observable user outcome** is ready to converge into `dev` or a PR is ready for merge/review.

Refresh live `dev` base/head, review the complete diff, make affected durable docs current, map risk dimensions -> required gates, execute/route deterministic evidence and add only affected critical E2E.

### RELEASE

`dev -> main` / release-candidate work is RELEASE. Use FULL plus release-critical package/E2E and any residual physical-device evidence required by the claim.

## Validation model

The selector reports:

`outcome -> risk dimensions -> required gates -> LEAN|SCOPED|STRONG|FULL -> executor`.

Profiles summarize selected gates; they are not fixed suite bundles. Ordinary contained app/UI work may stay SCOPED. Harness/Binder, privacy/security, persistence, manifest/package/R8 and other cross-boundary changes legitimately escalate. FULL is expected for release/selector/global-build/toolchain/unknown scope, not for every Local AI-labelled change.

Draft PRs may run ITERATION. A ready PR to `dev` runs INTEGRATION. `main` promotion runs RELEASE.

Unavailable deterministic Android gates are `REMOTE_AUTOMATED`; do not ask the user to become the Gradle runner.

## Evidence reuse

Before dispatching remote preflight, reuse successful evidence matching exact source HEAD, live target base, sufficient profile/required gates and material E2E identity.

PR recreation, draft/ready state or comments alone do not invalidate source evidence. Source edits, material base/dependency changes, changed required gates or stronger E2E requirements do.

Do not duplicate a green automatic PR `Validate` with an equivalent `/preflight` run.

## E2E / fidelity

Use the cheapest declared automated environment sufficient for the claim. UI evidence modes:

- `ASSERTIONS` — UI is incidental;
- `SCREENSHOTS` — stable visible layout/state/recovery/adaptive outcome matters;
- `FULL_MEDIA` — motion, timing/progression, navigation/transition sequence, lifecycle visibility or release acceptance is part of the claim.

RedactGuard mappings:

- `protect-text-document` and `protect-text-pdf` normally require screenshots;
- `recover-local-ai` requires FULL_MEDIA because availability/reconnection/lifecycle sequence is part of the claim;
- `harness-binder-roundtrip` is assertion/contract-oriented;
- emulator/two-APK evidence does not establish production ARM64 JNI/GGUF/memory/thermal/OEM behavior.

UI presence alone does not force video.

## Parallel development

Plan work as vertical user outcomes. Semantics, ViewModel ownership, UI recovery and tests may be parallel subtasks of one outcome rather than separate publication-grade PRs.

Use temporary parallel branches with non-conflicting ownership, then converge early onto a coherent feature/integration branch. Stacked publication is exceptional; pure stack-sync PRs are a process smell.

## Documentation

`docs/current-state.md` describes integrated/blocked/next truth, not every branch sync. Active workstreams are bounded/disposable.

During ITERATION durable docs may remain pending. At INTEGRATION every affected canonical owner must describe the exact candidate behavior. Delete completed workstreams after durable knowledge transfer by default.

## Failure discipline

Classify failures before editing: change regression, baseline, environment, flaky, base drift or assumption. Fix the owning invariant. Never weaken privacy/security/contract tests or add broad R8/keep workarounds merely to gain speed. Repeated failure requires a new hypothesis.

## Stop conditions

Surface rather than bypass: material ambiguity, privacy/security/trust conflicts, duplicate ownership, unsafe persistence/data lifecycle, stale affected docs at integration/release, required deterministic gates with no automation route, stronger environment claims than evidence supports, or requests to weaken legitimate gates merely for velocity.
