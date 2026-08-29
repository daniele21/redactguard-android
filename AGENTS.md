# RedactGuard agent guide

Use this file as a routing layer, not as a substitute for owning code or canonical documentation.

## Start here

1. Read `docs/current-state.md` for integrated, active and blocked work.
2. Read the closest canonical owner:
   - architecture -> `docs/architecture.md`;
   - durable behavior -> `docs/features/`;
   - rationale -> `docs/adr/`;
   - active implementation -> relevant `docs/workstreams/` file;
   - commands/publication readiness -> `.engineering/commands.json` plus `EXECUTION-CAPABILITY-CONTRACT.md`;
   - E2E environments/fidelity/residual gaps -> `.engineering/e2e.json`;
   - meaningful UX/UI -> `design/ux-contract.json` and `design/brand-kit.json`.
3. Read the closest scoped `AGENTS.md` if one exists below the edit path.
4. Use `skills/README.md` to route to the relevant recurring procedure, then read only that Skill.
5. Inspect owning code plus direct consumers/tests before changing a shared contract.

## Product and architecture invariants

- RedactGuard owns document ingestion, PII product policy, analysis orchestration, review/redaction and export.
- Harness owns application/use-case/preset/model/runtime/residency/Binder Host behavior and publishes the Consumer Android SDK.
- Do not add Harness source checkouts, composite builds, git submodules, copied Binder/runtime code or direct native/runtime ownership here.
- Do not bundle GGUF/GGML models.
- Sensitive document text, findings, reveal state and review decisions remain process-local by default.
- No silent cloud fallback.
- Image-only PDFs fail explicitly until a separate OCR/VLM capability owns that behavior.
- Known failure identity must remain stable/actionable across boundaries.
- Physical-device evidence is not interchangeable with JVM, CI or emulator evidence.
- Emulator E2E using `AnalysisRuntimePort` is not Consumer SDK/Binder/Harness/model evidence.

## Operating and E2E contracts

`.engineering/commands.json` is the repository command/publication router. `.engineering/e2e.json` owns E2E target/execution environments, fidelity classes, journeys and residual real-environment confirmation. Do not create a second command or E2E truth source.

Material distributable builds preserve build/source identity, fail-closed signing, immutable successful promotion, checksum/manifest lineage, bounded retention and build delta when active.

## Workstream discipline

Use `skills/plan-workstream/SKILL.md` only when persistent dependency/parallelism coordination is justified. Parallel changes may not write the same canonical owner without an explicit integration point. Transfer durable truth and delete completed workstreams by default.

## Product experience routing

For meaningful UI work follow `skills/design-product-experience/SKILL.md` in this order:

```text
user outcome -> task model -> IA / critical journey -> action hierarchy
-> progressive disclosure / defaults -> states / feedback / recovery
-> adaptive Android behavior -> accessibility -> design system
-> motion -> visual polish / graphics -> validation
```

Normal surfaces describe the privacy task. Harness/Binder/session/model internals belong in diagnostics only when they add recovery value.

## Change workflow

For meaningful changes use `skills/structured-change/SKILL.md`:

1. identify canonical owner and blast radius;
2. resolve material ambiguity from code/contracts/docs/ADRs/consumers/tests; ask only when alternatives materially change behavior/contracts/privacy/security/lifecycle/compatibility/acceptance/UX;
3. preserve the simplest design that keeps required invariants;
4. define failure, cancellation, cleanup and sensitive-data lifecycle where applicable;
5. change owner plus direct consumers/tests coherently;
6. use `skills/validate-change/SKILL.md` for cheapest useful evidence; for workflow/environment claims select journey/fidelity from `.engineering/e2e.json`; unavailable deterministic gates are `REMOTE_AUTOMATED`, not user-required;
7. classify failure cause/owner before editing; repeated failure needs a new falsifiable hypothesis;
8. update only durable current docs/experience contracts;
9. finalize completed workstreams with `skills/finalize-workstream/SKILL.md`;
10. before publication use `skills/preflight-change/SKILL.md` to refresh `dev`, review full diff, select `LEAN|SCOPED|STRONG|FULL`, select E2E fidelity and classify `AGENT_LOCAL|REMOTE_AUTOMATED|REAL_ENVIRONMENT`;
11. route unavailable deterministic gates through `skills/remote-preflight/SKILL.md` and `/preflight`; do not ask the user to run Gradle/R8/Lint/emulator work because the agent lacks Android tooling.

## Validation profiles

Selector: `python3 scripts/detect_ci_scope.py`, default `auto`.

- **LEAN** — docs/governance/metadata and cheap guards.
- **SCOPED** — contained app/UI/business-logic/test change: Spotless, debug compile, unit tests, Lint and affected AndroidTest assembly.
- **STRONG** — Harness/Binder, privacy/persistence/security, manifest, dependency, ProGuard/R8, AndroidTest, release/package/variant behavior.
- **FULL** — `dev -> main`, selector/CI/global Gradle/dependency inventory/toolchain changes, unknown executable scope or explicit full request.

`FULL` is exceptional. Stronger validation is allowed; silent downgrade below `auto` is forbidden.

## Environment fidelity

Execution capability and environment fidelity are independent:

- JVM/fakes prove only their declared logic/contract claims;
- `emulator-product-journeys` is `simulated_or_emulated`: it proves Android product orchestration/UI/document behavior, not real Harness/Binder/model/device behavior;
- `physical-two-apk` is `representative_physical` only when the exact identity-bearing gate runs;
- `harness-binder-roundtrip` remains an automation gap until a two-APK emulator gate reaches `dev`;
- accessibility, performance, thermal/resource and protected signing evidence remain separate when material.

Final physical validation should confirm residual environment facts, not be the first otherwise-automatable whole-product run.

## Validation and evidence

Never infer stronger results from weaker gates. Failure evidence stays privacy-safe: no document text, finding values, prompts, raw model/Binder payloads or sensitive filenames in normal diagnostics/fixtures.

## Publication readiness

Depth and execution/fidelity are separate:

- `READY_FOR_CI` — all selected deterministic gates ran agent-local and passed.
- `READY_FOR_REMOTE_PREFLIGHT` — semantic/base/diff/local checks passed; required gates remain `REMOTE_AUTOMATED` and the agent triggers `/preflight`.
- `AUTOMATED_PREFLIGHT_CONFIRMED` — all selected deterministic automated gates passed on exact head/base at required automated fidelity.
- `NOT_READY_FOR_AUTOMATED_PREFLIGHT` — required failure, unsafe scope/fidelity, missing remote route or blocker remains.

Physical two-APK, representative usability, thermal/performance and protected signing remain `REAL_ENVIRONMENT` and may still block claims that depend on them. Later edits/rebases/dependencies or material `dev`/environment movement invalidate affected evidence.

## Branch and PR discipline

- `dev` is integration; `main` is release/canonical when promoted.
- Branch from current integration head unless a workstream defines a stack point.
- Keep PRs bounded and avoid unrelated cleanup.
- PRs state invariants, exact head/base, validation profile/reason, E2E journey/environment/fidelity, local vs remote evidence and pending real-environment gaps.

## Stop conditions

Surface conflicts instead of improvising if a request duplicates truth, leaves material ambiguity, moves Harness/runtime/model ownership into RedactGuard, persists/logs sensitive data without an explicit requirement, adds cloud fallback, hides known failures, treats emulator evidence as physical evidence, creates a second design/token/command/E2E owner, bypasses validation, or removes a required gate merely to go green.
