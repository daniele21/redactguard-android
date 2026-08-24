# Documentation map

Use progressive disclosure. Start from `AGENTS.md`, then read only the canonical source required by the task.

| Question | Source |
| --- | --- |
| What exists and who owns it? | `architecture.md` |
| What is integrated, blocked or next? | `current-state.md` |
| How does durable product behavior work? | `features/` |
| Why was a material durable choice made? | `adr/` |
| What substantial implementation/evidence is still active? | `workstreams/` |
| How does setup/check/test/build/smoke/e2e/package/clean work? | `../.engineering/commands.json` |
| What product UX semantics/design ownership apply? | `../design/` when the `product-ui` profile is integrated |

## Active workstreams

- `workstreams/repo-template-sw-alignment.md` — engineering/product-ui convergence; Wave A is active in parallel.
- `workstreams/ombra-to-redactguard-migration.md` — repository extraction is complete; physical cutover and final Harness cleanup remain.
- `workstreams/failure-diagnostics-hardening.md` — repository failure contract is implemented; representative physical failure/recovery evidence remains.
- `workstreams/document-ingestion-v2.md` — pasted text/text-PDF convergence is implemented; physical ingestion smoke evidence remains.
- `workstreams/harness-control-plane-consumer-cutover.md` — multi-preset tolerance is integrated; assigned-use-case discovery and activation lifecycle depend on the corresponding Harness SDK/control-plane capabilities.

Keep ownership narrow: Harness owns application/use-case/preset/model/residency/Host telemetry administration; RedactGuard owns only the consumer-safe selection/lifecycle state exposed by the published SDK plus document/PII/review/export product behavior.

## Rules

- one canonical owner per durable fact;
- `current-state.md` records current truth and current blockers, not PR/commit history;
- workstreams are bounded working memory and are deleted after completion by default;
- implementation history belongs in Git;
- generated build/evidence artifacts are not project documentation;
- screenshots/traces/videos used for regression evidence remain bounded evidence, not a parallel design source of truth;
- sensitive user documents, prompts, finding values and device evidence containing user data are never committed as documentation fixtures.
