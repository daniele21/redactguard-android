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
| RTA-1 | Engineering baseline, local skills, repository verifiers/health gates | `.engineering/`, `skills/`, `scripts/verify_*`, `.github/` policy/health | — | yes | DONE |
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

RTA-1 through RTA-8 are integrated in canonical `dev`. The engineering baseline was merged by PR #53 as squash commit `371aec4242f23c45e428559dc62ad4c2862476a1` after exact-head `Validate` and `Repository health` both passed on `fc468ee35d1c4bf72b7be61ae9c5c8129ec78be2`.

That validated head included:

- repository structure and operating-contract verification;
- desired repository governance-policy verification;
- product-experience contract verification;
- documentation lifecycle/budget verification;
- instruction-context verification;
- Android formatting, helper syntax, failure contract, app/test compilation and JVM tests;
- native UI-test APK assembly;
- Android Lint;
- debug and minified-release APK assembly.

### RTA-1 durable result

- `.engineering/baseline.json` identifies standard 0.5.x, L1 target and `android` + `product-ui`;
- documentation and repository desired-state policies are machine-readable;
- local core engineering/product-experience skills are present;
- repository/operations/docs/context/product-experience/governance verifiers exist;
- PR template and repository-health CI make the adopted contract machine-visible;
- `.engineering/commands.json` routes real smoke/E2E intents rather than placeholders.

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

Repository-owned desired state is complete: `.engineering/repository-policy.json`, governance runbook and local verifier are integrated and passed repository-health verification.

Remain before `DONE`:

- apply the documented integration/release branch roles to the live GitHub repository;
- apply required checks/PR-only enforcement to `dev` and `main` as documented;
- verify the live settings after mutation.

The currently available repository tooling can verify live settings but does not expose the required branch-protection/default-branch mutation. Do not report desired-state policy as live enforcement.

## RTA-12 completion

RTA-12 remains blocked until RTA-9, RTA-10 and RTA-11 real-environment gates are satisfied and the final real-environment evidence agrees with the integrated repository contracts.

Architecture/ownership changes belong in `docs/architecture.md`; durable feature behavior in `docs/features/`; deterministic invariants in tests/tooling; product semantics/tokens in `design/`.

When all applicable evidence is complete, update `docs/current-state.md` and delete this workstream by default rather than retaining a completed implementation plan as history.
