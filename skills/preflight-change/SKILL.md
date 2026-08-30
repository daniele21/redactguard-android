---
name: preflight-change
description: Establish exact-head automated-validation readiness by resolving material ambiguity, verifying target-base freshness, reviewing the complete diff, proving durable documentation is current, selecting validation depth and E2E environment fidelity from blast radius, classifying execution capability and routing every required deterministic gate without turning the user into a test runner.
---

# Preflight Change

Use immediately before publishing/updating a PR. `validate-change` owns iterative validation; this Skill owns final documentation freshness, blast-radius selection, E2E fidelity selection, execution routing and readiness.

Read `EXECUTION-CAPABILITY-CONTRACT.md` when execution capability matters. Read `.engineering/e2e.json` whenever a complete workflow or platform/device/runtime/environment-dependent claim is affected. Read `docs/README.md` when documentation ownership or README impact is not obvious.

Governing rules:

> Validation depth follows blast radius: use the narrowest profile that proves the changed invariants.

> Code and durable documentation ship together: every affected canonical owner must describe the exact behavior being published.

> README identity and README usage are separate owners: do not rewrite stable mission/positioning for a usage-only change, and do not leave stale usage instructions because identity remains valid.

> E2E fidelity follows the claim: use the cheapest declared automated environment that represents the material target dimensions, then leave only irreducible gaps for real-environment confirmation.

> An automatable deterministic gate must not be delegated to the user merely because the current agent cannot run it locally.

## 1. Resolve ambiguity, base and diff

Resolve material ambiguity from canonical contracts/code/docs/ADRs/consumers/tests; record exact feature HEAD and intended `dev` base; review the complete diff for unrelated/generated/private files, weakened tests, duplicated ownership, stale docs/contracts, missed consumers, privacy/security/UX drift and stale E2E target/environment assumptions. Refresh affected evidence after any edit/rebase/dependency/base movement.

## 2. Assess documentation impact

Assess the resulting observable behavior, not filenames alone. Classify every plausible owner as `UPDATED` or `N/A` and give a short reason when impact was plausible but is `N/A`:

- `README_IDENTITY` — purpose, primary audience/outcome and stable positioning;
- `README_USAGE` — prerequisites, setup/run, public configuration, Consumer-facing/API/UI usage and examples;
- `FEATURE_DOCS` — durable non-obvious behavior/constraints/evidence;
- `ARCHITECTURE` — boundaries and ownership;
- `ADR` — durable decision/rationale;
- `SECURITY_DATA` — privacy, security, trust or data lifecycle;
- `OPERATIONS` — canonical command/operational semantics;
- `PRODUCT_EXPERIENCE` — `design/*` contracts when affected;
- `CURRENT_STATE` — integrated/blocker/next repository truth.

README identity does not change merely because a feature, command, configuration or implementation detail changed. README usage **must** change in the same PR when old setup/run/configuration/public examples would fail, omit a newly required step, advertise a removed path or otherwise mislead a current user/developer.

Existing feature docs must change in the same PR when the behavior they describe changes. Create a new feature doc only when durable non-obvious behavior is not sufficiently discoverable from public contracts, tests, code or architecture.

`verify_docs.py` may enforce structure/budgets but cannot prove semantic freshness. If an affected owner is stale, `DOCS_CURRENT_WITH_IMPLEMENTATION` is `FAIL` and publication readiness is blocked.

## 3. Select validation depth

Run the project selector from `.engineering/commands.json` using `auto` and record `LEAN | SCOPED | STRONG | FULL`, reason and affected jobs.

RedactGuard guidance:

- `LEAN` — docs/governance/metadata-only and cheap repository guards.
- `SCOPED` — contained app/UI/business-logic change with focused Spotless/compile/unit/lint evidence.
- `STRONG` — Harness consumer/Binder integration, privacy/persistence/security boundaries, manifest, dependency, AndroidTest, R8/ProGuard, release/package/variant behavior.
- `FULL` — promotion/release, selector/CI/global Gradle/dependency inventory/toolchain changes, unknown executable paths or explicit full request.

Unknown executable scope fails safe stronger. Selector/build-inventory changes force `FULL`. Do not silently downgrade below `auto`; stronger validation is always allowed.

## 4. Select E2E journey and fidelity

When the profile/claim requires E2E, read `.engineering/e2e.json`. For each affected journey:

1. identify the complete outcome and material target dimensions;
2. select the smallest relevant journey subset;
3. select the cheapest declared automated environment whose fidelity is sufficient;
4. require built APK/package execution when distribution/install behavior matters;
5. escalate only when the claim depends on a dimension missing from the cheaper environment;
6. preserve declared residual gaps and `required|conditional` real-environment confirmation.

For RedactGuard, `emulator-product-journeys` is `simulated_or_emulated`: it uses production app/document/UI code but substitutes the Harness boundary. `physical-two-apk` is `representative_physical` only when actually executed with exact identity. Do not upgrade emulator evidence to Binder/model/device evidence.

The `harness-binder-roundtrip` journey currently has an explicit automation gap: merged CI does not yet install/run both APKs. Keep the physical gate as residual evidence until a repository-owned automated two-APK emulator environment is integrated; do not hide this gap.

## 5. Classify execution capability

Assign each selected gate to:

- `AGENT_LOCAL` — executable by the current agent on exact HEAD;
- `REMOTE_AUTOMATED` — deterministic/automatable but unavailable in the agent environment;
- `REAL_ENVIRONMENT` — genuinely requires representative hardware, protected authority/external environment or manual evidence.

Gradle, Kotlin compile, Lint, R8/minification, unit tests, AndroidTest assembly and emulator E2E are `REMOTE_AUTOMATED`, not `REAL_ENVIRONMENT`, when ChatGPT lacks Android tooling. Report E2E with both executor class and environment ID/fidelity class.

Run all local gates. If selected deterministic gates remain remote and semantic/base/diff/documentation checks pass, status is `READY_FOR_REMOTE_PREFLIGHT` and control passes to `skills/remote-preflight/SKILL.md`; do not ask the user to run Gradle.

## 6. Failure discipline and parity

Classify failures as `CHANGE_REGRESSION`, `BASELINE_FAILURE`, `ENVIRONMENT`, `FLAKY`, `BASE_DRIFT` or `ASSUMPTION`; identify violated invariant/owner before editing. Never suppress a legitimate gate merely to go green. Repeated failure requires a new hypothesis. Re-run documentation impact, profile and fidelity selection after material repairs.

Local and remote validation must invoke the same project-owned commands/scripts/selector semantics. If remote runs are routinely broader than required, improve the selector. If a physical run repeatedly finds automatable workflow regressions, improve the declared automated E2E environment instead of accepting device testing as the first complete-system check.

## Output

```text
HEAD: <revision>
TARGET: <branch>@<revision>
AMBIGUITY: PASS|FAIL
BASE_FRESHNESS: PASS|FAIL
FULL_DIFF_REVIEW: PASS|FAIL
DOCUMENTATION_IMPACT:
  README_IDENTITY: UPDATED|N/A <reason when useful>
  README_USAGE: UPDATED|N/A <reason when useful>
  FEATURE_DOCS: UPDATED|N/A <reason when useful>
  ARCHITECTURE: UPDATED|N/A <reason when useful>
  ADR: UPDATED|N/A <reason when useful>
  SECURITY_DATA: UPDATED|N/A <reason when useful>
  OPERATIONS: UPDATED|N/A <reason when useful>
  PRODUCT_EXPERIENCE: UPDATED|N/A <reason when useful>
  CURRENT_STATE: UPDATED|N/A <reason when useful>
DOCS_CURRENT_WITH_IMPLEMENTATION: PASS|FAIL
VALIDATION_PROFILE: LEAN|SCOPED|STRONG|FULL
PROFILE_REASON: <reason>
EXECUTION_CAPABILITY: local|mixed|remote-only
E2E_JOURNEYS:
  <journey>: <environment-id> / <fidelity-class> / PASS|FAIL|PENDING|N/A
E2E_RESIDUAL_GAPS:
  <journey>: <gap or N/A>
AGENT_LOCAL:
  <gate>: PASS|FAIL|N/A
REMOTE_AUTOMATED:
  <gate>: PASS|FAIL|PENDING|N/A
REAL_ENVIRONMENT:
  <gate>: PASS|PENDING|N/A
READINESS: READY_FOR_CI|READY_FOR_REMOTE_PREFLIGHT|AUTOMATED_PREFLIGHT_CONFIRMED|NOT_READY_FOR_AUTOMATED_PREFLIGHT
```

Documentation must be current for `READY_FOR_CI`, `READY_FOR_REMOTE_PREFLIGHT` and `AUTOMATED_PREFLIGHT_CONFIRMED`. Any later edit, rebase/replay, dependency change or material `dev`/environment relationship change invalidates affected evidence and requires documentation impact to be rechecked. A known-red draft may be published only when explicitly wanted and may not claim readiness.
