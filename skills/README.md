# Core project-local Skills

These Skills are copied into RedactGuard and versioned with the project. They encode recurring procedures that should not inflate the root `AGENTS.md`.

Core set:

- `plan-workstream` — coordinate bounded dependency-aware work only when persistence is justified;
- `structured-change` — preserve ownership, simplicity, resource/failure/data invariants and resolve material ambiguity;
- `design-product-experience` — reason through meaningful UX/UI work in the correct order before implementation/polish;
- `validate-change` — choose the narrowest sufficient iterative validation and diagnose failures at the owning invariant;
- `preflight-change` — establish exact-head `READY_FOR_CI` after target-base freshness, complete-diff review and required local deterministic gates;
- `finalize-workstream` — transfer durable knowledge and delete completed plans by default;
- `review-reference-quality` — perform an L0/L1/L2 quality review before important milestones.

Record intentional local customization in `.engineering/baseline.json` so future baseline migrations merge rather than overwrite it.

Before publication, `preflight-change` owns the readiness decision; `validate-change` remains the iterative validation procedure.

Do not create a Skill for one-off instructions. A Skill is justified when a procedure recurs, is conditional, has non-obvious ordering/hazards, or saves substantial repeated agent context.
