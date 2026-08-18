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

## Active workstreams

- Cross-repository extraction from OMBRA: `workstreams/ombra-to-redactguard-migration.md`.
- Failure diagnostics/recovery hardening: `workstreams/failure-diagnostics-hardening.md`.
- Text-first document ingestion: `workstreams/document-ingestion-v2.md`.
- Harness Host Control Plane consumer cutover, including multiple/custom host presets and future activation lifecycle: `workstreams/harness-control-plane-consumer-cutover.md`.

Keep ownership narrow: Harness owns application/use-case/preset/model/residency/host telemetry administration; RedactGuard owns only the consumer-facing selection/lifecycle state allowed by the published SDK plus its document/PII product behavior.

## Rules

- one canonical owner per fact;
- current-state records current truth, not implementation history;
- workstream plans are deleted after completion unless an independent audit reason requires retention;
- generated build/evidence artifacts are not committed as project documentation;
- sensitive user documents, prompts/findings and device evidence containing user data are never documentation fixtures.
