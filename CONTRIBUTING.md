# Contributing

RedactGuard uses `dev` as the integration branch and focused workstream/slice branches for parallel changes. `main` is the release/canonical branch when a release is promoted.

Before editing:

1. read `AGENTS.md` and `docs/current-state.md`;
2. find the canonical owner in architecture/features/design/operating contract;
3. read the relevant active workstream when the change belongs to one;
4. inspect direct consumers and tests before modifying shared behavior.

## Change discipline

Keep ownership explicit and prefer the smallest change that preserves required invariants. Do not add dependencies, abstraction layers, caches, services, UI component families or persistence without a concrete need.

Preserve the product boundaries: RedactGuard owns document/PII/review/export behavior; Harness owns local-model runtime and control-plane behavior exposed through the published Consumer SDK.

Sensitive document text/findings/review state remain process-local by default, diagnostics remain privacy-safe, and there is no silent cloud fallback.

Resolve material ambiguity from canonical code/contracts/docs/ADRs/consumers/tests before implementation. If two reasonable interpretations still materially change behavior, contracts, persistence, privacy/security, resource/lifecycle semantics, compatibility, acceptance criteria or meaningful UX, ask the user/owner rather than silently selecting one.

## UX/UI changes

When `product-ui` is adopted, read `design/ux-contract.json` and `design/brand-kit.json`. Structural UX must settle user outcome, task model, critical journey, hierarchy, disclosure/defaults, states/recovery, adaptive behavior and accessibility before design-system/motion/polish work.

## Validation depth

Use `.engineering/commands.json` as the canonical repository command map and `scripts/detect_ci_scope.py` as the project-owned blast-radius selector. `auto` is the normal path:

- `LEAN` — documentation, governance and metadata plus cheap repository/contract guards; do not initialize the Android SDK.
- `SCOPED` — contained app/UI/business-logic/test changes; run Spotless, debug compilation, unit tests, Lint and debug assembly.
- `STRONG` — Harness consumer/Binder integration, PII/redaction/privacy/persistence/security boundaries, manifest, app dependency/build changes, ProGuard/R8, AndroidTest or release/package/variant behavior; add AndroidTest assembly and unsigned minified release/R8 validation.
- `FULL` — `dev -> main` promotion/release, validation selector/workflow changes, global Gradle/dependency inventory/toolchain changes, unknown executable scope or an explicit full request.

`FULL` is exceptional on ordinary feature PRs. Stronger explicit validation is allowed. Silent downgrade below the profile selected by `auto` is forbidden.

If a narrow profile misses a deterministic failure in a materially affected component, strengthen the selector/dependency mapping so the same class escalates automatically next time; do not permanently make every PR full.

## Execution capability

Validation depth and execution location are separate decisions. Required evidence is classified as:

- `AGENT_LOCAL` — the current coding agent can execute the gate on exact HEAD;
- `REMOTE_AUTOMATED` — deterministic and automatable but unavailable in the current agent environment;
- `REAL_ENVIRONMENT` — genuinely requires representative hardware, protected authority/external environment or manual evidence.

An automatable deterministic gate must not be delegated to the user solely because the coding agent lacks Android tooling. Gradle, Kotlin compilation, Lint, R8/minification, unit tests, AndroidTest APK assembly and unsigned debug/release builds are `REMOTE_AUTOMATED` when unavailable agent-local.

When a gate fails, classify it as current-change regression, baseline failure, environment/toolchain issue, flaky behavior, stale-base effect or incorrect assumption/contract before editing production code. Fix the owning invariant rather than applying unexplained patches. If the same gate fails again after a fix, re-evaluate the hypothesis before another edit.

Do not equate:

```text
unit/integration tests != smoke != E2E != physical-device evidence
```

## Pre-publication readiness

Before publishing/updating a PR, use `skills/preflight-change/SKILL.md`. Refresh the intended `dev` revision, review the complete diff, record exact head/base identity, resolve material ambiguity, select the blast-radius profile and classify the selected gates by execution capability.

If required deterministic gates cannot run agent-local, use `skills/remote-preflight/SKILL.md` and the repository `/preflight` automation rather than asking the user to execute them manually.

Readiness is one of:

- `READY_FOR_CI`;
- `READY_FOR_REMOTE_PREFLIGHT`;
- `AUTOMATED_PREFLIGHT_CONFIRMED`;
- `NOT_READY_FOR_AUTOMATED_PREFLIGHT`.

Physical-device/two-APK, representative usability, thermal/performance and protected signing evidence remains `REAL_ENVIRONMENT`; report it as `PASS`, `PENDING` or `N/A` and never infer it from host/CI evidence.

## Pull requests

PRs should be bounded to one workstream slice/owner where practical and state:

- what changed and why;
- affected invariants/contracts;
- product-experience impact when applicable;
- build/runtime/artifact impact when applicable;
- exact preflight HEAD and target-base revision;
- selected `LEAN|SCOPED|STRONG|FULL` profile, reason and affected jobs/components;
- `AGENT_LOCAL` evidence as `PASS|FAIL|N/A`;
- `REMOTE_AUTOMATED` evidence/run identity as `PASS|FAIL|PENDING|N/A`;
- `REAL_ENVIRONMENT` evidence as `PASS|PENDING|N/A`;
- final readiness state;
- durable docs/design contracts changed or why none are needed.

Promotion to `main` requires `FULL` automated validation on the exact candidate plus any stronger real-environment evidence required by the promoted claims.

Completed workstream plans are deleted by default after durable current behavior has moved to architecture/features/ADR/tests and `docs/current-state.md` has been updated.
