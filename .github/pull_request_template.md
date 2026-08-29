## What changed

<!-- Small, concrete summary. -->

## Why

<!-- Problem/outcome and important tradeoffs. -->

## Invariants / risk

<!-- Public contracts, privacy/data, failure, security, migration or operating-lifecycle implications. Write N/A when truly not applicable. -->

## Product experience

<!-- If user-facing behavior is affected: task/IA/progressive disclosure, critical states/feedback/recovery, accessibility/adaptive layout, design-system/brand implications and critical journeys. Otherwise N/A. -->

## Build / runtime / artifact lifecycle

<!-- If applicable: canonical command intents affected; build identity; artifact manifest/checksum/build delta/retention; package/cleanup implications. Otherwise N/A. -->

## Pre-publication readiness

- HEAD: `<revision>`
- TARGET: `dev@<revision>`
- AMBIGUITY: `PASS|FAIL`
- BASE_FRESHNESS: `PASS|FAIL`
- FULL_DIFF_REVIEW: `PASS|FAIL`
- READINESS: `READY_FOR_CI|READY_FOR_REMOTE_PREFLIGHT|AUTOMATED_PREFLIGHT_CONFIRMED|NOT_READY_FOR_AUTOMATED_PREFLIGHT`

A known-red draft must be explicit and may not claim automated readiness.

## Validation profile

- AUTO resolution: `LEAN|SCOPED|STRONG|FULL`
- Reason: `<selector reason>`
- Affected jobs/components: `<scope>`
- Override: `N/A|strong|full` and why

`FULL` is exceptional for ordinary feature PRs. Stronger explicit validation is allowed; weaker-than-auto requires an explicit exception and justification.

## Agent-local validation

<!-- Selected deterministic gates the current agent executed directly: PASS/FAIL/N/A. -->

## Remote automated validation

<!-- Selected deterministic gates unavailable agent-local: PASS/FAIL/PENDING/N/A plus /preflight run identity. Gradle/R8/Lint/build/emulator is not user-required merely because ChatGPT lacks Android tooling. -->

## E2E environment / fidelity evidence

<!-- For each affected critical journey: .engineering/e2e.json environment ID, fidelity class, PASS/FAIL/PENDING/N/A and residual gaps. Emulator evidence must not be upgraded into physical Harness/Binder/model evidence. -->

## Real-environment evidence

<!-- Physical two-APK/hardware/protected signing/accessibility/usability/performance evidence automation cannot replace: PASS/PENDING/N/A, tied to the residual gap/claim it supports. -->

## Product-experience evidence

<!-- Accessibility/adaptive/visual/usability evidence when affected. Otherwise N/A. -->

## Documentation / design lifecycle

<!-- Durable docs/design contracts updated, or why none are required. Completed workstream deleted/finalized when applicable. Generated screenshots are evidence, not default durable design truth. -->
