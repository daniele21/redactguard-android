---
name: validate-change
description: Run the cheapest useful validation during iteration, expand by risk at integration, and keep E2E/environment evidence proportional to the actual claim.
---

# Validate Change

Read `.engineering/commands.json`; read `.engineering/e2e.json` only when a complete workflow/environment claim is affected.

## ITERATION — default

Goal: falsify the current edit quickly.

- Spotless/format for the touched surface;
- affected debug compile;
- focused unit/component tests;
- direct contract/privacy tests only when the changed boundary needs them.

Do not default to lint + debug APK + AndroidTest APK + R8 + E2E + exact-head preflight. A draft PR remains ITERATION.

## INTEGRATION

When the vertical outcome is observable and ready to converge, map:

`changed outcome -> risk dimensions -> required gates -> profile shorthand -> executor`.

Add only gates implied by risk. Ordinary contained UI/application work can stay scoped; Harness/Binder, privacy/security, persistence and release-sensitive inputs escalate. `LEAN/SCOPED/STRONG/FULL` summarize required gates rather than owning a fixed giant suite.

## RELEASE

Use FULL plus release-critical package/E2E/residual-device evidence. Main promotion is release-stage work.

## RedactGuard fidelity

- `protect-text-document` and `protect-text-pdf` normally use `SCREENSHOTS` for stable visible outcomes.
- `recover-local-ai` uses `FULL_MEDIA` because lifecycle/reconnection sequence is part of the claim.
- `harness-binder-roundtrip` is headless/contract-oriented and uses assertions; emulator evidence does not establish ARM64 native/model/thermal behavior.
- physical two-APK evidence remains separate when the claim requires it.

UI evidence modes are `ASSERTIONS`, `SCREENSHOTS`, `FULL_MEDIA`; UI presence alone does not force video.

## Failure loop

Classify failures as change regression, baseline, environment, flaky, base drift or assumption. Fix the owning invariant; never weaken privacy/security/contracts/tests merely to gain speed. Repeated failure requires a new hypothesis.

Unavailable deterministic gates are `REMOTE_AUTOMATED`, not user work. Hand off to `preflight-change` only when the slice becomes INTEGRATION/RELEASE ready.
