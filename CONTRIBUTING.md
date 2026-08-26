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

## Validation

Use `.engineering/commands.json` as the canonical repository command map. While iterating, run the narrowest deterministic gate that can falsify the change quickly; before finalization expand to the correct repository/integration/E2E/device evidence for the claim.

When a gate fails, classify it as current-change regression, baseline failure, environment/toolchain issue, flaky behavior, stale-base effect or incorrect assumption/contract before editing production code. Fix the owning invariant rather than applying unexplained patches. If the same gate fails again after a fix, re-evaluate the hypothesis before another edit.

Do not equate:

```text
unit/integration tests != smoke != E2E != physical-device evidence
```

If required evidence cannot run, report it as pending rather than passed.

## Pre-publication readiness

Before pushing/opening/updating a PR for normal readiness confirmation, use `skills/preflight-change/SKILL.md`. Refresh the intended `dev` revision, review the complete diff, record exact head/base identity, resolve material ambiguity and run every required locally reproducible deterministic gate selected by blast radius.

CI should confirm these deterministic semantics rather than be the normal debugger for formatting, compilation, tests or lint. Device/two-APK evidence that genuinely cannot run locally must be explicitly `PENDING` and still blocks stronger device claims.

## Pull requests

PRs should be bounded to one workstream slice/owner where practical and state:

- what changed and why;
- affected invariants/contracts;
- product-experience impact when applicable;
- build/runtime/artifact impact when applicable;
- exact preflight HEAD and target-base revision;
- local gates as `PASS`, `FAIL`, `PENDING` or `N/A`;
- `READY_FOR_CI` vs `NOT_READY_FOR_CI`;
- required CI/device evidence still pending;
- durable docs/design contracts changed or why none are needed.

Completed workstream plans are deleted by default after durable current behavior has moved to architecture/features/ADR/tests and `docs/current-state.md` has been updated.
