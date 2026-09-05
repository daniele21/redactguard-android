# Validation Execution Capability Contract

Version: 0.3.2

RedactGuard adopts the repo-template-sw 0.9.2 delivery model: **delivery stage**, **validation depth**, **execution capability** and **environment fidelity** are separate axes.

## Governing rules

> Automation executes automatable work; the user is not the fallback runner because an agent lacks Android tooling.

> Optimize for sufficient confidence per feedback time: cheap falsification at ITERATION, automated risk-based proof at INTEGRATION, reference-grade proof plus required real-environment acceptance at RELEASE.

> Reuse trusted equivalent evidence before starting another expensive run.

## Execution classes

- `AGENT_LOCAL` — current agent can execute the deterministic gate directly.
- `REMOTE_AUTOMATED` — deterministic/automatable but unavailable locally; repository automation owns it.
- `REAL_ENVIRONMENT` — genuinely requires representative physical hardware, protected authority/environment or human judgement.

Gradle, Kotlin compile, lint, unit tests, AndroidTest assembly, R8 and unsigned build/package work do not become `REAL_ENVIRONMENT` merely because the current agent lacks Android tooling.

## Delivery stages

- `ITERATION` — fast falsification; exact-head/full-diff/docs/preflight/release E2E are not defaults.
- `INTEGRATION` — coherent observable outcome ready for `dev`; exact head/base, full diff, affected docs, selected risk gates and affected critical **automated** journeys. Required physical/target evidence is recorded as `DEFERRED_TO_RELEASE` and does not block the feature PR.
- `RELEASE` — `main`/release candidate; FULL plus release-critical E2E/artifacts and every required residual `REAL_ENVIRONMENT` gate passing.

A draft collaboration PR may remain ITERATION. A ready PR to `dev` is INTEGRATION. Physical-device runs may still be used early to diagnose explicitly device-specific defects; they are not the normal integration gate.

## Risk -> gates -> profile

The selector reports risk dimensions and concrete required gates. `LEAN`, `SCOPED`, `STRONG`, `FULL` are shorthand summaries rather than monolithic suites.

Typical RedactGuard escalation risks include Harness/Binder integration, privacy/persistence/security, manifest/dependencies, AndroidTest, R8/ProGuard, package/variant behavior and selector/global-build changes. FULL is expected for release and validation/global-build/unknown scope, not every feature.

## E2E routing

The canonical product E2E command is emulator-backed. Core text/PDF/recovery journeys already retain screenshot checkpoints and continuous journey videos, so material UI/UX integration claims can satisfy `FULL_MEDIA` without a physical device.

The two-APK emulator owns the real Consumer SDK/Binder/Host integration claim with a deterministic native/model backend. Its remaining ARM64 JNI/GGUF, physical memory, thermal and OEM gaps stay explicit and move to RELEASE when marked required.

## Evidence identity and reuse

Before new remote work, search trusted successful evidence.

For the integration candidate, reusable proof normally matches exact source HEAD, source Git tree, target/base relationship, required gates/profile and material E2E environment/evidence claim. PR recreation, draft/ready state, labels or comments alone do not invalidate source proof.

### Post-merge tree-equivalent reuse

After a content-preserving squash/rebase into `dev`, the push workflow may reuse the successful integration proof despite a new commit SHA only when:

1. the merged commit Git tree exactly matches the validated candidate tree;
2. `github.event.before` is exactly the target/base revision used by that validation;
3. required gates/profile remain identical or weaker;
4. the repository-owned evidence artifact is current and trusted.

A moved base, changed tree, broader gates, direct push without matching evidence or expired evidence validates normally. This is **content-equivalent reuse**, not a claim that the old run executed on the new commit object.

RELEASE remains exact-candidate/reference-grade.

## Remote preflight

`/preflight auto` is the default; stronger overrides may increase evidence. It searches exact-head evidence first and runs only missing/stale/insufficient deterministic gates. Post-merge tree reuse is owned by integration-branch CI, not by weakening the candidate preflight.

`AUTOMATED_PREFLIGHT_CONFIRMED` is sufficient for integration when automated gates pass and residual real-environment evidence is explicitly deferred. It is not `RELEASE_READY` while a required real-environment gate remains pending.

## Security

Require trusted requesters, exact-head pinning for new runs, same-repository PRs by default, read-only/no write credentials while change-branch code executes, no production/signing/deployment secrets, separate reporting permission where practical, and bounded evidence retention.

## Failure loop

Inspect evidence, classify change regression/baseline/environment/flaky/base drift/assumption, identify the owner, patch it, reselect risks/gates, invalidate only affected proof and rerun only what remains. Never downgrade/suppress a legitimate gate to obtain green status.

Missing remote automation is `AUTOMATION_CAPABILITY_GAP`; unsafe scope classification is `VALIDATION_SCOPE_GAP`.
