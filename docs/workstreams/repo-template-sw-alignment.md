# repo-template-sw alignment

Status: active
Owner: redactguard-android
Read when: aligning repository engineering/product UX with `daniele21/repo-template-sw` 0.5.x

## Goal

Reach a truthful, machine-checkable L1 `repo-template-sw` baseline with `android` and `product-ui` profiles while preserving RedactGuard privacy, failure and Harness ownership boundaries. Use selected low-cost L2 controls where they close real risk.

## Non-goals

- OCR/VLM or cloud parsing;
- moving model/runtime/Binder Host ownership into RedactGuard;
- redesigning Harness control-plane/runtime contracts;
- persisting sensitive document/findings/review state to simplify UI;
- decorative UI work before task hierarchy and recovery semantics are settled.

## Invariants

- document text/findings/review state remain process-local by default;
- no silent cloud fallback;
- RedactGuard owns document/PII/review/export; Harness owns model/runtime/control plane;
- known failure identity remains actionable end to end;
- CI/emulator evidence is never reported as physical-device evidence;
- meaningful UI follows `outcome -> task -> journey -> hierarchy -> disclosure/defaults -> states/recovery -> adaptive -> accessibility -> design system -> motion/polish -> evidence`;
- parallel slices do not write the same canonical owner without an explicit integration point.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| RTA-1 | Engineering baseline, local skills, repository verifiers/health gates | `.engineering/`, `skills/`, `scripts/verify_*`, `.github/` policy/health | — | yes | ACTIVE |
| RTA-2 | Agent routing and documentation lifecycle | `AGENTS.md`, `CONTRIBUTING.md`, `docs/` routing/current workstreams | — | yes | ACTIVE |
| RTA-3 | Build identity and artifact lineage | `app/build.gradle.kts`, artifact/package/release scripts | — | yes | ACTIVE |
| RTA-4 | Product-experience contract | `design/ux-contract.json`, `design/README.md`, `design/reference/` | — | yes | ACTIVE |
| RTA-5 | Brand/design tokens and Compose theme primitives | `design/brand-kit.json`, semantic theme/components | RTA-4 | yes | BLOCKED |
| RTA-6 | Critical product journey/hierarchy | product-flow state/screens/microcopy | RTA-4 | yes | BLOCKED |
| RTA-7 | Adaptive Android + accessibility | adaptive layout/semantics/tests | RTA-4 | yes | BLOCKED |
| RTA-8 | Integrate design system, restrained motion and polish | product UI composition | RTA-5,RTA-6,RTA-7 | no | BLOCKED |
| RTA-9 | UX regression evidence | Compose UI/accessibility/adaptive/selective visual evidence | RTA-6,RTA-7,RTA-8 | yes | BLOCKED |
| RTA-10 | Real `smoke` and `e2e` operating intents | command wiring/device cleanup/evidence | RTA-1,RTA-3,RTA-4 | yes | BLOCKED |
| RTA-11 | Canonical branch/required-check governance | repository settings/runbook evidence | RTA-1 | yes | BLOCKED |
| RTA-12 | Exact-head convergence/finalization | integration, durable handoff, delete this plan | RTA-2..RTA-11 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Current executable slices

`RTA-1`, `RTA-2`, `RTA-3`, `RTA-4` execute from the same integration head with separate write ownership.

### RTA-1 acceptance

- `.engineering/baseline.json` identifies standard 0.5.x, L1 target and `android` + `product-ui`;
- documentation policy and local core skills are present;
- repository/operations/docs/context/product-experience verifiers exist;
- PR template and repository-health CI make the adopted contract machine-visible;
- `.engineering/commands.json` uses the current operating schema rather than a second command wrapper.

Validation: repository-health verifiers. During parallel adoption, cross-lane failures remain truthful until their contracts land.

### RTA-2 acceptance

- root `AGENTS.md` routes to canonical owners, commands, design contracts, workstreams and validation;
- `CONTRIBUTING.md` describes branch/PR/evidence discipline;
- `docs/current-state.md` contains current truth rather than PR history;
- workstreams stay within configured budgets and only remain active for genuine unresolved code/evidence gates;
- feature/ADR/workstream lifecycle entrypoints exist;
- this workstream remains linked once from current state while active.

Validation: `verify_docs.py` and `verify_agent_context.py` after RTA-1 integration.

### RTA-3 acceptance

- material builds carry build ID distinct from product version plus full source revision/dirty state;
- distributable promotion is staging -> validate -> immutable success;
- successful artifacts include semantic filename, manifest, SHA-256 and build delta against previous comparable success;
- local retention keeps at most two successful builds per variant;
- failed builds do not create valid-looking promoted artifacts;
- signed release remains fail-closed and uses the same lineage contract.

Validation: two comparable packaged builds have distinct IDs; manifest/checksum/delta verify; failure creates no success artifact; retention is bounded.

### RTA-4 acceptance

`design/ux-contract.json` owns user/job, critical journey, action hierarchy, disclosure/defaults, user-vs-diagnostic terminology, critical states, feedback/recovery, adaptive contexts, accessibility, motion purpose, reference views and critical E2E journeys.

Normal UI uses task language such as local-AI availability rather than exposing Harness/Binder implementation vocabulary without user value.

Validation: product-experience verifier after RTA-5 supplies the brand contract, plus manual contract review.

## Wave B

After RTA-4 is stable, run in parallel:

```text
RTA-5 design system     RTA-6 structural UX     RTA-7 adaptive/accessibility
```

RTA-5 owns tokens/components, RTA-6 owns task flow/hierarchy/microcopy, RTA-7 owns adaptive/accessibility behavior. RTA-8 integrates them; motion/graphics remain last.

## Wave C

RTA-9 (UX evidence), RTA-10 (smoke/E2E) and RTA-11 (branch governance) can run in parallel when prerequisites are satisfied. Physical two-APK evidence remains a separate real-environment gate.

## Integration notes

- RTA-1 repository-health is intentionally strict: it may remain red on its isolated branch until RTA-2/RTA-3/RTA-4/RTA-5 provide the required contracts. Do not weaken verifiers for temporary convergence convenience.
- RTA-3 implements artifact behavior; RTA-1 owns the declarative `.engineering/commands.json` contract that routes to it.
- `product-ui` adoption becomes fully green only when RTA-4 UX semantics and RTA-5 brand/design-system contract both exist.

## Durable destinations and completion

Architecture/ownership changes -> `docs/architecture.md`; durable feature behavior -> `docs/features/`; material rationale -> ADR; deterministic invariants -> tests/tooling; product semantics/tokens -> `design/`.

RTA-12 is complete only when applicable repository, Android, product-experience, artifact, E2E and required real-device evidence agree at the exact integration head. Then update `docs/current-state.md` and delete this workstream by default.
