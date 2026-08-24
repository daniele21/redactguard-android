# Active workstreams

This directory contains only substantial implementation work that still needs persistent dependency, parallelism or evidence coordination.

Use `skills/plan-workstream/SKILL.md` and `_template.md` when a persistent plan is justified.

Rules:

- one active file per bounded workstream;
- one outcome-oriented goal, explicit non-goals and material invariants;
- stable slice IDs with non-conflicting `Owns/writes` boundaries;
- allowed states only: `READY`, `ACTIVE`, `BLOCKED`, `DONE`;
- current executable slices must be obvious without reading narrative history;
- targeted validation belongs beside the slice that requires it;
- stay within `.engineering/documentation-policy.json` budgets;
- link an active workstream once from `docs/current-state.md`;
- do not keep separate plan/progress/status diaries for the same work;
- completed implementation history belongs in Git, not current documentation.

A workstream remains active when a genuine required gate is still pending, including representative physical-device evidence. When all required code, failure/resource behavior, integration and evidence are complete, transfer only durable current truth to architecture/features/ADR/tests, update `docs/current-state.md` and delete the workstream by default.
