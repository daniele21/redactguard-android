# Documentation map

Use progressive disclosure. Start from `AGENTS.md`, then read only the canonical source required by the task.

| Question | Source |
| --- | --- |
| What exists and who owns it? | `architecture.md` |
| What is integrated, blocked or next? | `current-state.md` |
| How does a durable product boundary behave? | `features/` |
| Why was a durable architectural choice made? | `adr/` |
| What substantial implementation is active now? | `workstreams/` |
| How does the repository setup/run/check/test/build/smoke/package/clean? | `../.engineering/commands.json` |

## Active workstream

The cross-repository extraction from OMBRA is owned by `workstreams/ombra-to-redactguard-migration.md`. It remains active only until behavior/evidence have converged and durable knowledge has been transferred into architecture/features/runbooks.

## Rules

- one canonical owner per fact;
- current-state records current truth, not implementation history;
- workstream plans are deleted after completion unless an independent audit reason requires retention;
- generated build/evidence artifacts are not committed as project documentation;
- sensitive user documents, prompts/findings and device evidence containing user data are never documentation fixtures.
