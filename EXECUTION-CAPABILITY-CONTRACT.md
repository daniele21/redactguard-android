# Validation Execution Capability Contract

Version: 0.2.0

This contract defines **who executes required validation** and **how much automated validation is justified**. It adopts `repo-template-sw` 0.7.0 semantics without weakening required evidence.

The governing rules are:

> Automation should execute automatable work. A human must not become the fallback test runner merely because a coding agent lacks a local shell, checkout, SDK or build environment.

> Validation depth follows blast radius. Do not run a full repository/release matrix when a narrower automated profile can prove the changed invariants.

## Execution classes

- `AGENT_LOCAL` — the agent can execute the gate directly on the exact current head.
- `REMOTE_AUTOMATED` — deterministic and automatable but unavailable in the agent environment; repository-owned automation executes it.
- `REAL_ENVIRONMENT` — genuinely requires representative hardware, protected authority, an external environment or manual evidence.

Gradle, Kotlin compilation, Lint, R8/minification, unit tests, AndroidTest APK assembly and unsigned build/package work are `REMOTE_AUTOMATED`, not `REAL_ENVIRONMENT`, when ChatGPT lacks Android tooling.

## Validation depth profiles

- `LEAN` — docs/governance/metadata-only or cheap universal guards.
- `SCOPED` — contained application implementation plus focused compile/unit/lint evidence.
- `STRONG` — Harness consumer/Binder boundaries, persistence/privacy/security, manifest, dependency, R8/ProGuard, packaging/variant or other release-sensitive changes.
- `FULL` — promotion/release, selector/CI/global-build/dependency-inventory/toolchain changes, unknown executable paths, explicit full validation or cases where narrowing cannot be trusted.

`FULL` is exceptional on ordinary feature PRs. Automatic escalation is allowed; silent downgrade below `auto` is forbidden.

## Automatic profile selection

The project selector must compare exact base/head paths, keep docs-only changes cheap, classify normal app changes as `SCOPED`, escalate release/cross-boundary changes to `STRONG`, fail safe stronger on unknown executable paths, force `FULL` when validation/build selection machinery changes, and report profile/reason/jobs.

## No-human-runner principle

An automatable deterministic gate MUST NOT be delegated to the user solely because the coding agent lacks local execution capability.

```text
agent lacks Android SDK
-> classify Gradle/R8 gate as REMOTE_AUTOMATED
-> select profile from blast radius
-> trigger repository-owned remote preflight with profile=auto
-> inspect result/logs
-> fix owning cause
-> re-evaluate profile
-> retrigger automation
```

## Agent-triggerable remote preflight

The default request is `/preflight auto`; `/preflight strong` and `/preflight full` may increase evidence. Remote preflight resolves the exact PR/head, selects the profile unless explicitly strengthened, executes canonical project validation, reports profile/reason/PASS/FAIL, is retriggerable and remains privacy-safe.

## Security model

Require trusted requesters, exact-head pinning, same-repository PRs by default, read-only/no write credentials while change-branch code executes, no signing/production/deployment secrets, separate write-capable reporting, bounded timeout and bounded evidence retention.

## Readiness

- `READY_FOR_CI`
- `READY_FOR_REMOTE_PREFLIGHT`
- `AUTOMATED_PREFLIGHT_CONFIRMED`
- `NOT_READY_FOR_AUTOMATED_PREFLIGHT`

`REAL_ENVIRONMENT` evidence is separate and may remain `PENDING`, while still blocking any stronger device/product claim that depends on it.

## Failure loop

```text
remote failure
-> inspect logs/evidence
-> classify failure
-> identify violated invariant + owner
-> patch owning cause
-> re-evaluate blast radius/profile
-> review diff/base impact
-> retrigger remote preflight
```

Do not ask the user to rerun the same automatable command between iterations. Missing remote automation is `AUTOMATION_CAPABILITY_GAP`; unsafe scope classification is `VALIDATION_SCOPE_GAP`.
