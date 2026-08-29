---
name: validate-change
description: Select the narrowest sufficient validation for a change while iterating, diagnose failures at their owning invariant, and identify the correct final gate by blast radius without confusing unavailable agent-local execution with a human testing requirement or confusing emulator evidence with target-environment evidence.
---

# Validate Change

## Principle

Do not run the entire repository for every edit, and do not stop at a local unit test when a shared contract, runtime boundary or critical user experience changed. Validation follows blast radius and the strength of the claim.

Use `.engineering/commands.json` as the canonical repository-level command routing surface. Read `.engineering/e2e.json` when a complete workflow, platform/device/runtime assumption or environment-dependent claim is affected. When `product-ui` is adopted and user-facing behavior changes, also read `design/ux-contract.json` and `design/brand-kit.json`.

This Skill owns iterative validation selection. `preflight-change` owns final exact-head execution classification/readiness. `remote-preflight` owns deterministic remote execution when the current agent lacks an equivalent local environment.

## Validation ladder

### Level A — local iteration

Use formatter/linter, focused unit/component tests and module compilation for private implementation inside one owner. Run these directly when possible; otherwise record deterministic unavailable gates as `REMOTE_AUTOMATED` candidates rather than asking the user to run them.

### Level B — direct consumers

Add direct consumer/contract tests, persistence/privacy checks, affected UI/transport compilation and component-state tests when a contract or behavior affects callers/adapters.

### Level C — integration/repository

For public contracts, multiple domains, build/configuration, CI/tooling or broad dependencies add canonical `check`/`test`, integration/contract tests, build when relevant, and repository/operating/E2E-fidelity/product-experience guards.

### Level D — end-to-end/product flow

Use E2E when the claim crosses a complete user/system workflow boundary and lower-level tests cannot establish the final outcome. Select the smallest relevant critical journey and the cheapest declared automated environment in `.engineering/e2e.json` that truthfully represents the changed dimensions. Run against the built artifact when package/install behavior is material, retain bounded identity-bearing evidence, and verify owned cleanup.

Execution capability and fidelity are separate. `REMOTE_AUTOMATED` says where the gate executed; `simulated_or_emulated`, `representative_virtual`, `representative_physical` and `target_environment` say what environment claim it supports. A green Android emulator is not physical-device evidence.

### Level E — real environment / representative evidence

Use only for claims deterministic automation cannot truthfully prove or where `.engineering/e2e.json` declares residual confirmation: physical Android/OEM behavior, real Harness Consumer SDK/Binder/model execution, protected signing, representative accessibility/usability, or performance/resource/thermal behavior.

A final physical run should primarily confirm residual fidelity gaps. If it repeatedly discovers ordinary product-flow failures that emulator/CI could reproduce, strengthen automated E2E instead of normalizing the user/device run as the first whole-system test.

## E2E environment fidelity

When Level D or E matters, answer from `.engineering/e2e.json`:

1. Which critical journey owns the claim?
2. Which target-environment dimensions matter?
3. Which automated environment is the cheapest sufficient one?
4. Which residual gaps still require physical/target confirmation?

Prefer only the needed progression:

```text
lower-level tests
-> automated E2E
-> built/package artifact E2E when material
-> highest practical automated fidelity
-> residual real/target-environment confirmation
```

Do not mechanically run every rung. If a required journey has no automated environment, keep its explicit `automation_gap_reason`; do not silently convert it into an undocumented manual test.

## RedactGuard fidelity boundaries

- JVM/unit/contract evidence does not establish Android framework behavior.
- `emulator-product-journeys` establishes simulated/emulated Android app orchestration and production UI/document behavior, but its deterministic `AnalysisRuntimePort` does not establish Consumer SDK/Binder/Harness/model behavior.
- `physical-two-apk` establishes representative physical same-signer RedactGuard/Harness behavior when actually executed with recorded identity; one device does not automatically represent all supported devices.
- Accessibility, performance, thermal, resource and protected-release claims remain separate evidence unless their explicit protocol was executed.

## Product experience validation

When user-facing behavior changes, validate the affected task/journey, hierarchy, progressive disclosure/defaults, critical states/feedback/recovery, accessibility/adaptive behavior, design-system reuse and motion/visual semantics at proportional depth. A screenshot can support a visual claim but cannot by itself prove interaction, accessibility, recovery, adaptive behavior or usability.

## Smoke vs E2E

A build is not smoke and smoke is not E2E. `smoke` proves minimal install/launch/path viability; `e2e` proves a complete critical workflow across the assembled system. Use both when both claims matter.

## Failure diagnosis

Classify a red gate before editing as current-change regression, baseline failure, environment/toolchain issue, flaky behavior, base drift or incorrect assumption. Identify the violated invariant and owner; never weaken a legitimate test merely to go green. Repeated failure requires a new falsifiable hypothesis.

## Operational validation

When runtime/build/package/E2E/lifecycle behavior changes, verify build/source identity, immutable successful artifact promotion, manifest/checksum/build delta/retention, privacy-safe bounded evidence, correct environment/fidelity identity, and cleanup of project-owned temporary/device state on success and failure.

## Workflow

1. Identify changed owner, user-visible impact and blast radius.
2. Read the nearest agent guide and `.engineering/commands.json`; read `.engineering/e2e.json` for complete workflows/environment claims and design contracts for UI work.
3. Run the cheapest deterministic gate that can falsify the edit when executable locally.
4. On failure classify cause/owner before editing.
5. Expand only when boundaries or final integration require it.
6. For E2E select the declared journey and cheapest sufficient fidelity; escalate only for material missing dimensions.
7. Preserve residual physical evidence separately.
8. Route unavailable deterministic gates as `REMOTE_AUTOMATED`, not user-required.
9. Report exact gates, E2E environment/fidelity and remaining evidence.
10. Hand final evidence to `preflight-change` before publication.

## Output

Report each applicable gate as `PASS`, `FAIL`, `PENDING` or `N/A`; classify pending gates as `REMOTE_AUTOMATED` or `REAL_ENVIRONMENT`. For E2E record the `.engineering/e2e.json` environment ID/fidelity class and residual gaps. Absence of agent-local execution does not make a deterministic gate manual.
