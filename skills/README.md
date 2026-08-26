# Core project-local Skills

These Skills are copied into RedactGuard and versioned with the project. They encode recurring procedures that should not inflate the root `AGENTS.md`.

Core set:

- `plan-workstream` — coordinate bounded dependency-aware work only when persistence is justified;
- `structured-change` — preserve ownership, simplicity, resource/failure/data invariants and resolve material ambiguity;
- `design-product-experience` — reason through meaningful UX/UI work in the correct order before implementation/polish;
- `validate-change` — choose the narrowest sufficient iterative validation, diagnose failures at the owning invariant and mark unavailable deterministic gates for remote automation rather than human execution;
- `preflight-change` — establish exact-head readiness after base/diff review, `LEAN|SCOPED|STRONG|FULL` selection and execution-capability classification;
- `remote-preflight` — trigger and close repository-owned deterministic remote validation when the current agent lacks equivalent Android execution capability;
- `finalize-workstream` — transfer durable knowledge and delete completed plans by default;
- `review-reference-quality` — perform an L0/L1/L2 quality review before important milestones.

Record intentional local customization in `.engineering/baseline.json` so future baseline migrations merge rather than overwrite it.

Before publication, `preflight-change` owns profile/executor/readiness selection. When it reports `READY_FOR_REMOTE_PREFLIGHT`, use `remote-preflight` immediately. Automatable Gradle/R8/Lint/build work must not fall back to the user merely because the current agent lacks Android tooling.

Do not create a Skill for one-off instructions. A Skill is justified when a procedure recurs, is conditional, has non-obvious ordering/hazards, or saves substantial repeated agent context.
