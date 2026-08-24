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
| RTA-2 | Agent routing and documentation lifecycle | `AGENTS.md`, `CONTRIBUTING.md`, `docs/` routing/current workstreams | — | yes | DONE |
| RTA-3 | Build identity and artifact lineage | `app/build.gradle.kts`, artifact/package/release scripts | — | yes | DONE |
| RTA-4 | Product-experience contract | `design/ux-contract.json`, `design/README.md`, `design/reference/` | — | yes | DONE |
| RTA-5 | Brand/design tokens and Compose theme primitives | `design/brand-kit.json`, semantic theme/components | RTA-4 | yes | DONE |
| RTA-6 | Critical product journey/hierarchy | product-flow state/screens/microcopy | RTA-4 | yes | DONE |
| RTA-7 | Adaptive Android + accessibility | adaptive layout/semantics/tests | RTA-4 | yes | DONE |
| RTA-8 | Integrate design system, restrained motion and polish | product UI composition | RTA-5,RTA-6,RTA-7 | no | DONE |
| RTA-9 | UX regression evidence | Compose UI/accessibility/adaptive evidence | RTA-6,RTA-7,RTA-8 | yes | ACTIVE |
| RTA-10 | Real `smoke` and `e2e` operating intents | command wiring/device cleanup/evidence | RTA-1,RTA-3,RTA-4 | yes | ACTIVE |
| RTA-11 | Canonical branch/required-check governance | repository policy/runbook/live settings evidence | RTA-1 | yes | ACTIVE |
| RTA-12 | Exact-head convergence/finalization | integration, durable handoff, delete this plan | RTA-2..RTA-11 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Converged repository implementation

The repository-side implementation for RTA-2 through RTA-8 is integrated and validated. RTA-1 now contains the converged `dev` product-experience/device-helper state plus the engineering baseline and RTA-11 desired-state governance policy.

`Repository health` has already passed repository structure, operating contract, product-experience contract, documentation lifecycle and agent-context verification on the converged RTA-1 base before the final ledger update. The final exact-head `Validate` + `Repository health` rerun remains the RTA-1 completion gate.

### RTA-1 acceptance

- `.engineering/baseline.json` identifies standard 0.5.x, L1 target and `android` + `product-ui`;
- documentation policy and local core skills are present;
- repository/operations/docs/context/product-experience verifiers exist;
- PR template and repository-health CI make the adopted contract machine-visible;
- `.engineering/commands.json` uses the current operating schema and routes real smoke/E2E intents.

### RTA-3 durable result

Material builds carry build ID plus source identity; distributable promotion is staged and immutable; promoted artifacts include manifest/SHA-256/build delta; retention is bounded; signed release stays fail-closed.

### RTA-4 through RTA-8 durable result

`design/ux-contract.json` and `design/brand-kit.json` own the product-experience/design contract. RedactGuard uses task-first local-AI language, semantic theme/tokens, bounded adaptive layouts, accessibility semantics, privacy-preserving review defaults and restrained motion/polish.

## Remaining evidence gates

### RTA-9 — product-experience evidence

Repository implementation is complete: native Compose instrumentation tests compile/package in CI and `docs/evidence/product-experience.md` defines the bounded evidence procedure.

Remain before `DONE`:

- execute instrumentation on an explicit Android target;
- record representative TalkBack, large-text and compact/medium/expanded behavior on a physical device;
- retain only privacy-safe synthetic evidence with exact source/build/device identity.

### RTA-10 — physical smoke/E2E

Repository implementation is complete: smoke and guided physical two-APK helpers are executable, fail-closed and owned-cleanup aware.

Remain before `DONE`:

- execute the same-signer Harness + RedactGuard physical flow on a real device;
- cover pasted text and text PDF, local analysis, review, cancellation/recovery, Host absence/death/reconnect, export, independent reopen and cleanup;
- record exact app/build/device identity without sensitive document contents.

### RTA-11 — live repository governance

Repository-owned desired state is complete: `.engineering/repository-policy.json`, governance runbook and local verifier are integrated.

Remain before `DONE`:

- make `dev` the intended protected integration branch and keep `main` as release branch according to the documented policy;
- apply required checks/PR-only enforcement to the live GitHub repository;
- verify the live settings after mutation.

The currently available repository tooling can verify live settings but does not expose the required branch-protection/default-branch mutation. Do not report desired-state policy as live enforcement.

## RTA-12 completion

RTA-12 remains blocked until RTA-9, RTA-10 and RTA-11 real-environment gates are satisfied and the final exact-head repository/Android checks agree.

Architecture/ownership changes belong in `docs/architecture.md`; durable feature behavior in `docs/features/`; deterministic invariants in tests/tooling; product semantics/tokens in `design/`.

When all applicable evidence is complete, update `docs/current-state.md` and delete this workstream by default rather than retaining a completed implementation plan as history.
