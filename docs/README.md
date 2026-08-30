# Documentation map

Use progressive disclosure. Start from `AGENTS.md`, then read only the canonical source required by the task.

| Question | Source |
| --- | --- |
| What is RedactGuard and why does it exist? | README identity sections: title/summary/why, audience and primary outcome |
| How does a person install, configure, run or use it? | README usage sections: prerequisites/setup/run/configuration/public examples |
| What exists and who owns it? | `architecture.md` |
| What is integrated, blocked or next? | `current-state.md` |
| How does durable product behavior work? | `features/` |
| Why was a material durable choice made? | `adr/` |
| What substantial implementation/evidence is still active? | `workstreams/` |
| How does setup/check/test/build/smoke/e2e/package/clean work? | `../.engineering/commands.json` |
| What product UX semantics/design ownership apply? | `../design/` when the `product-ui` profile is integrated |

## Documentation impact contract

Code and durable documentation ship together. A meaningful change is not complete until its documentation impact has been assessed and every affected canonical owner describes the exact behavior being published.

Treat the README as two semantic owners:

- **Identity** — purpose, primary audience/outcome and stable positioning. Do not rewrite this merely because implementation, commands, configuration or a feature workflow changed.
- **Usage** — prerequisites, setup, run/start, configuration, public Consumer-facing/API/UI flow and copy-paste examples. Update these sections in the same change whenever the current instructions would otherwise become incomplete, wrong or misleading.

A change may legitimately report `README_IDENTITY: N/A` and `README_USAGE: UPDATED`.

For other durable impact use the existing owner: feature behavior -> `features/`; architecture/ownership -> `architecture.md`; rationale -> `adr/`; trust/privacy/data lifecycle -> `SECURITY.md` and/or the owning architecture/feature doc; command semantics -> `.engineering/commands.json`; product experience -> `design/*`; integrated/blocker/next truth -> `current-state.md`.

Do not update every document mechanically. During preflight classify plausible owners as `UPDATED` or `N/A` and require `DOCS_CURRENT_WITH_IMPLEMENTATION: PASS` before readiness.

## Active workstreams

- `workstreams/repo-template-sw-alignment.md` — engineering/product-ui convergence; Wave A is active in parallel.
- `workstreams/ombra-to-redactguard-migration.md` — repository extraction is complete; physical cutover and final Harness cleanup remain.
- `workstreams/failure-diagnostics-hardening.md` — repository failure contract is implemented; representative physical failure/recovery evidence remains.
- `workstreams/document-ingestion-v2.md` — pasted text/text-PDF convergence is implemented; physical ingestion smoke evidence remains.
- `workstreams/harness-control-plane-consumer-cutover.md` — multi-preset tolerance is integrated; assigned-use-case discovery and activation lifecycle depend on the corresponding Harness SDK/control-plane capabilities.

Keep ownership narrow: Harness owns application/use-case/preset/model/residency/Host telemetry administration; RedactGuard owns only the consumer-safe selection/lifecycle state exposed by the published SDK plus document/PII/review/export product behavior.

## Rules

- one canonical owner per durable fact;
- existing feature docs are updated in the same change when the durable behavior they describe changes;
- create a new feature doc only when non-obvious durable behavior is not sufficiently discoverable from code/contracts/tests/architecture;
- `current-state.md` records current truth and current blockers, not PR/commit history;
- workstreams are bounded working memory and are deleted after completion by default;
- implementation history belongs in Git;
- generated build/evidence artifacts are not project documentation;
- screenshots/traces/videos used for regression evidence remain bounded evidence, not a parallel design source of truth;
- sensitive user documents, prompts, finding values and device evidence containing user data are never committed as documentation fixtures.
