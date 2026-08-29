# RedactGuard agent guide

Use this file as a routing layer, not as a substitute for the owning code or canonical documentation.

## Start here

1. Read `docs/current-state.md` for what is integrated, active and blocked.
2. Read the closest canonical owner for the task:
   - architecture/ownership -> `docs/architecture.md`;
   - durable feature behavior -> `docs/features/`;
   - durable rationale -> `docs/adr/`;
   - substantial active implementation -> the relevant `docs/workstreams/` file;
   - setup/check/test/build/e2e/package/cleanup/publication readiness -> `.engineering/commands.json` plus `EXECUTION-CAPABILITY-CONTRACT.md`;
   - E2E target/execution environments, fidelity and residual physical-device gaps -> `.engineering/e2e.json`;
   - meaningful UX/UI -> `design/ux-contract.json` and `design/brand-kit.json` once present.
3. Read the closest scoped `AGENTS.md` if one exists below the path you will edit.
4. Use `skills/README.md` to route to the relevant recurring procedure, then read only that Skill.
5. Inspect the owning code plus direct consumers/tests before changing a shared contract.

## Product and architecture invariants

- RedactGuard owns document ingestion, PII product policy, analysis orchestration, review/redaction and export.
- Harness owns application/use-case/preset/model/runtime/residency/Binder Host behavior and publishes the Consumer Android SDK.
- Do not add Harness source checkouts, composite builds, git submodules, copied Binder/runtime code or direct native/runtime ownership to this repository.
- Do not bundle GGUF/GGML model artifacts.
- Sensitive document text, findings, reveal state and review decisions remain process-local by default.
- No silent cloud fallback.
- Image-only PDFs fail explicitly until a separate OCR/VLM capability owns that behavior.
- Known failure identity must remain stable and actionable across boundaries; do not collapse a known cause into a generic bucket for UI convenience.
- Physical-device evidence is not interchangeable with JVM, CI or emulator evidence.
- Emulator product E2E that substitutes `AnalysisRuntimePort` must not be presented as Consumer SDK/Binder/Harness/model evidence.

## Operating and E2E contracts

`.engineering/commands.json` is the only repository-level command router and owns the publication gate. `.engineering/e2e.json` owns E2E target/execution environments, fidelity classes, critical journeys and residual real-environment confirmation. Do not create a second undocumented command or E2E truth source.

Common intents are `setup`, `doctor`, `dev`, `check`, `test`, `e2e`, `build`, `smoke`, `package`, `stop` and `clean`. If an intent is not implemented yet, keep that state explicit rather than inventing a weak substitute.

Material distributable builds must preserve build/source identity, fail-closed signing, immutable successful promotion, checksum/manifest lineage, bounded retention and a build delta when the corresponding contract is active.

## Workstream discipline

Use `skills/plan-workstream/SKILL.md` when work needs persistent dependency/parallelism coordination. A workstream must have one outcome-oriented goal/non-goals, stable slice IDs and writes boundaries, only `READY|ACTIVE|BLOCKED|DONE`, executable dependencies/validation, bounded documentation cost and one link from `docs/current-state.md` while active.

Parallel changes may not write the same canonical owner without an explicit integration point. Completed plans are working memory: transfer durable truth, update current state and delete the workstream by default.

## Product experience routing

When `product-ui` is adopted, meaningful UI work follows `skills/design-product-experience/SKILL.md` and this order:

```text
user outcome -> task model -> IA / critical journey -> action hierarchy
-> progressive disclosure / defaults -> states / feedback / recovery
-> adaptive Android behavior -> accessibility -> design system
-> motion -> visual polish / graphics -> validation
```

Normal surfaces should describe the user's privacy task. Harness/Binder/session/model internals belong in diagnostics only when they add real recovery value. Do not use animation/graphics to compensate for unresolved hierarchy or feedback.

## Change workflow

For meaningful changes use `skills/structured-change/SKILL.md` before editing and before finalizing:

1. identify the canonical owner and blast radius;
2. resolve material ambiguity from canonical code/contracts/docs/ADRs/consumers/tests; ask the user only if reasonable alternatives still materially change behavior, contracts, persistence, privacy/security, lifecycle, compatibility, acceptance criteria or meaningful UX;
3. preserve the simplest design that keeps required invariants;
4. define failure, cancellation, cleanup and sensitive-data lifecycle where applicable;
5. change the owner plus direct consumers/tests coherently;
6. use `skills/validate-change/SKILL.md` for the cheapest useful iterative evidence; when complete workflow/environment claims matter, select the journey and fidelity from `.engineering/e2e.json`; if a deterministic gate cannot run in the current agent environment, mark it `REMOTE_AUTOMATED`, not user-required;
7. on failure, classify cause/owner before editing; repeated failure requires a new falsifiable hypothesis;
8. update only durable current documentation/experience contracts;
9. finalize completed workstreams with `skills/finalize-workstream/SKILL.md`;
10. use `skills/preflight-change/SKILL.md` before publication to refresh `dev`, review the complete diff, select `LEAN|SCOPED|STRONG|FULL`, select required E2E fidelity, and classify gates as `AGENT_LOCAL`, `REMOTE_AUTOMATED` or `REAL_ENVIRONMENT`;
11. when selected deterministic gates are unavailable locally, immediately use `skills/remote-preflight/SKILL.md` and `/preflight`; do not ask the user to run Gradle/R8/Lint/emulator work merely because the current agent lacks Android tooling.

## Validation profiles

The canonical selector is `python3 scripts/detect_ci_scope.py`; `auto` is the default.

- **LEAN** — docs/governance/metadata plus repository/contract guards; no Android SDK initialization.
- **SCOPED** — contained application/UI/business-logic/test change: Spotless, debug compile, unit tests, Lint and debug/AndroidTest assembly when affected.
- **STRONG** — Harness consumer/Binder integration, PII/redaction/privacy/persistence/security boundaries, manifest, dependency, ProGuard/R8, AndroidTest or release/package/variant behavior; adds minified release/R8 where relevant.
- **FULL** — `dev -> main` promotion/release, selector/CI/global Gradle/dependency inventory/toolchain changes, unknown executable scope or explicit full request.

`FULL` is exceptional. Stronger explicit validation is allowed; silent downgrade below `auto` is forbidden. If a narrow profile misses a deterministic impacted failure, strengthen scope mapping rather than making every PR full.

## Environment fidelity

Execution capability and environment fidelity are independent:

- JVM/fakes prove only their declared contract/logic claims;
- `emulator-product-journeys` is `simulated_or_emulated` and proves deterministic Android product orchestration/UI/document behavior, not real Harness/Binder/model/device behavior;
- `physical-two-apk` is `representative_physical` only when the exact identity-bearing gate is executed;
- `harness-binder-roundtrip` remains a declared automation gap until a two-APK emulator gate is integrated into `dev`;
- accessibility, performance, thermal/resource and protected signing evidence remain separate when those claims are material.

Final physical-device validation should confirm residual environment-specific facts, not become the first time an otherwise automatable full product journey is exercised.

## Validation and evidence

Never infer a stronger result from a weaker gate. Distinguish focused/unit/component tests, integration/contract tests, repository `check`/`test`/`build`, smoke, E2E, accessibility/adaptive/visual evidence and physical-device evidence.

Failure evidence must remain privacy-safe: no document text, finding values, prompts, raw model/Binder payloads or sensitive filenames in normal diagnostics or committed fixtures.

## Publication readiness

Two questions are separate: **how much** validation is needed and **where/at what fidelity** it executes.

- `READY_FOR_CI` — every deterministic gate required by the selected profile could run agent-local and passed.
- `READY_FOR_REMOTE_PREFLIGHT` — semantic/base/diff and available local checks passed but required selected gates are `REMOTE_AUTOMATED`; the agent triggers `/preflight` rather than delegating them to the user.
- `AUTOMATED_PREFLIGHT_CONFIRMED` — all deterministic automated gates required by the selected profile passed on the exact head/base at the required declared automated fidelity.
- `NOT_READY_FOR_AUTOMATED_PREFLIGHT` — required failure, unsafe scope/fidelity, missing remote route or another blocker remains.

Physical-device/two-APK behavior, representative usability, thermal/performance and protected signing evidence remain `REAL_ENVIRONMENT`. They may be `PENDING` after automated preflight and still block stronger claims that depend on them.

Any later edit, rebase/replay, dependency change or material `dev`/environment movement invalidates affected readiness evidence.

## Branch and PR discipline

- `dev` is the integration branch; `main` is the release/canonical branch when promoted.
- Create focused branches from current integration head unless an active workstream defines a stacked point.
- Keep PRs bounded to one owner/slice where practical and avoid unrelated cleanup.
- PR descriptions state affected invariants, exact head/base, selected validation profile/reason, E2E journey/environment/fidelity, local vs remote automated evidence and pending real-environment gaps.

## Stop conditions

Surface the conflict instead of improvising if a requested change would duplicate a source of truth, leave material ambiguity unresolved, move Harness/runtime/model ownership into RedactGuard, persist/log sensitive document data without an explicit privacy requirement, add cloud fallback, hide a known failure cause, treat CI/emulator evidence as physical-device evidence, create a second design/token/command/E2E owner, bypass publication/validation, or remove a still-required evidence gate merely to go green.
