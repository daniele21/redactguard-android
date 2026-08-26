# RedactGuard agent guide

Use this file as a routing layer, not as a substitute for the owning code or canonical documentation.

## Start here

1. Read `docs/current-state.md` for what is integrated, active and blocked.
2. Read the closest canonical owner for the task:
   - architecture/ownership -> `docs/architecture.md`;
   - durable feature behavior -> `docs/features/`;
   - durable rationale -> `docs/adr/`;
   - substantial active implementation -> the relevant `docs/workstreams/` file;
   - setup/check/test/build/e2e/package/cleanup/publication readiness -> `.engineering/commands.json`;
   - meaningful UX/UI -> `design/ux-contract.json` and `design/brand-kit.json` once present.
3. Read the closest scoped `AGENTS.md` if one exists below the path you will edit.
4. Inspect the owning code plus direct consumers/tests before changing a shared contract.

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

## Operating contract

`.engineering/commands.json` is the only repository-level command router and owns the publication gate. Do not create a second undocumented setup/build/test/package workflow.

Common intents are `setup`, `doctor`, `dev`, `check`, `test`, `e2e`, `build`, `smoke`, `package`, `stop` and `clean`. If an intent is not implemented yet, keep that state explicit rather than inventing a weak substitute.

Material distributable builds must preserve build/source identity, fail-closed signing, immutable successful promotion, checksum/manifest lineage, bounded retention and a build delta when the corresponding contract is active.

## Workstream discipline

Use `skills/plan-workstream/SKILL.md` when work needs persistent dependency/parallelism coordination. A workstream must:

- have one outcome-oriented goal and explicit non-goals;
- define stable slice IDs and `Owns/writes` boundaries;
- use only `READY`, `ACTIVE`, `BLOCKED`, `DONE`;
- make executable slices and dependencies obvious;
- put targeted validation next to the owning slice;
- stay within `.engineering/documentation-policy.json` budgets;
- be linked once from `docs/current-state.md` while active.

Parallel changes may not write the same canonical owner without an explicit integration point.

Completed plans are working memory: transfer durable truth to architecture/features/ADR/tests, update current state and delete the workstream by default. Git is the implementation history.

## Product experience routing

When the `product-ui` profile is adopted, meaningful UI work follows `skills/design-product-experience/SKILL.md` and this order:

```text
user outcome -> task model -> IA / critical journey -> action hierarchy
-> progressive disclosure / defaults -> states / feedback / recovery
-> adaptive Android behavior -> accessibility -> design system
-> motion -> visual polish / graphics -> validation
```

Normal surfaces should describe the user's privacy task. Harness/Binder/session/model internals belong in diagnostics only when they add real recovery value.

Do not introduce raw repeated visual values or one-off components when a semantic token/component owner exists. Do not use animation or graphics to compensate for unresolved hierarchy or feedback.

## Change workflow

For meaningful changes use `skills/structured-change/SKILL.md` before editing and before finalizing:

1. identify the canonical owner and blast radius;
2. resolve material ambiguity from canonical code/contracts/docs/ADRs/consumers/tests; if reasonable alternatives still materially change behavior, contracts, persistence, privacy/security, lifecycle, compatibility, acceptance criteria or meaningful UX, ask the user before implementing that decision;
3. preserve the simplest design that keeps required invariants;
4. define failure, cancellation, cleanup and sensitive-data lifecycle where applicable;
5. change the owner plus direct consumers/tests coherently;
6. run the narrowest useful validation while iterating;
7. on failure, classify the cause and owning invariant before editing again; repeated failure requires a new falsifiable hypothesis, not another symptom patch;
8. expand validation according to `skills/validate-change/SKILL.md`;
9. update only durable current documentation/experience contracts;
10. finalize completed workstreams with `skills/finalize-workstream/SKILL.md`;
11. use `skills/preflight-change/SKILL.md` before publication to refresh `dev`, review the complete diff, run every required locally reproducible deterministic gate on the exact head and declare CI/device-only evidence.

## Validation and evidence

Never infer a stronger result from a weaker gate. Distinguish:

- focused/unit/component tests;
- integration/contract tests;
- repository `check`/`test`/`build`;
- `smoke` minimal viability;
- `e2e` complete critical journey;
- accessibility/adaptive/visual evidence;
- physical-device evidence.

Report unavailable required evidence as `PENDING`, not `PASS`. Do not weaken a legitimate test or contract merely to make a branch green.

Failure evidence must remain privacy-safe: no document text, finding values, prompts, raw model/Binder payloads or sensitive filenames in normal diagnostics or committed fixtures.

## Publication readiness

The governing rule is **CI should confirm, not discover** locally reproducible deterministic failures. `READY_FOR_CI` requires no unresolved material ambiguity, current `dev` base identity, complete-diff review, exact-head identity and all required local deterministic gates passing.

For Android changes, preflight includes applicable Spotless/formatting, Kotlin compilation, unit/contract tests, Android Lint and assemble/package checks. `check` mirrors the deterministic compile/lint/helper-contract gates exercised by CI; physical-device/two-APK evidence remains explicitly `PENDING` when unavailable locally and still blocks stronger device claims.

Any later edit, rebase/replay, dependency change or material `dev` movement invalidates affected readiness evidence.

## Branch and PR discipline

- `dev` is the integration branch; `main` is the release/canonical branch when promoted.
- Create focused branches from the current integration head unless the active workstream defines a stacked integration point.
- Keep PRs bounded to one owner/slice where practical.
- Do not mix unrelated cleanup into a behavioral change.
- PR descriptions must state affected invariants, exact preflight head/base, validation and pending real-environment evidence.

## Stop conditions

Surface the conflict instead of improvising if a requested change would:

- duplicate an existing source of truth;
- leave a material product/contract ambiguity unresolved;
- move Harness/runtime/model ownership into RedactGuard;
- persist or log sensitive document data without an explicit privacy-reviewed requirement;
- add cloud fallback;
- hide a known failure cause;
- treat CI/emulator evidence as physical-device evidence;
- create a second design/token/command owner;
- bypass the publication gate or canonical validation;
- require motion/polish before task hierarchy and recovery semantics are settled;
- remove a still-required workstream/evidence gate only to make repository health pass.
