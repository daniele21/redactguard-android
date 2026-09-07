# Contributing

RedactGuard uses `dev` as the integration branch and focused workstream/slice branches for parallel changes. `main` is the stable/release branch.

Before editing:

1. read `AGENTS.md` and the canonical owner needed for the task;
2. for material product behavior/capability/strategy work, read `.engineering/product.json` + `docs/product.md` and use `skills/shape-product-change/SKILL.md` before substantial implementation;
3. read `docs/current-state.md` when integrated/blocker/next repository state matters;
4. read the relevant active workstream only when the change belongs to one;
5. inspect direct consumers/tests before modifying shared behavior.

## Product-change discipline

Product depth is independent from engineering effort and validation depth.

- `PRODUCT_NONE` — implementation-only maintenance; no product ceremony.
- `PRODUCT_LOCAL` — settled local behavior; state affected user, desired outcome and acceptance only.
- `PRODUCT_FEATURE` — establish user/problem/outcome, material value/usability/feasibility/viability risks/assumptions, non-goals, quality constraints and success before substantial implementation.
- `PRODUCT_STRATEGIC` — use stronger evidence/alternatives/rollout/compatibility reasoning for broad product-boundary, target-user, trust or value changes.

Discovery may conclude `BUILD`, `NARROW_SCOPE`, `CHOOSE_ALTERNATIVE` or `DO_NOT_BUILD`. Do not create a parallel PRD/progress system: when persistent coordination is justified, carry compact product intent in the existing workstream.

Shipping proves delivery, not product impact. Add post-release learning only where real use is needed to resolve material uncertainty; analytics are not mandatory.

## Change discipline

Keep ownership explicit and prefer the smallest change that preserves required invariants. Do not add dependencies, abstraction layers, caches, services, UI component families or persistence without a concrete need.

Preserve the durable product boundary in `docs/product.md`: RedactGuard owns document/PII/review/redaction/export behavior; Harnex owns model/runtime/control-plane behavior exposed through the published Consumer SDK.

Sensitive document text/findings/review state remain process-local by default, diagnostics remain privacy-safe, protection-critical uncertainty fails closed, and there is no silent cloud fallback. Users remain the authority over which findings become redactions.

Translate material product quality promises into measurable technical invariants/evidence at the owning domain rather than duplicating implementation detail in `docs/product.md`.

Resolve material ambiguity from canonical product/code/contracts/docs/ADRs/consumers/tests before implementation. Surface conflicts that materially change product intent, behavior, contracts, persistence, privacy/security, resource/lifecycle semantics, compatibility, acceptance criteria or meaningful UX instead of silently selecting one.

## UX/UI changes

When `product-ui` is adopted, shaped product work passes user/problem/outcome/constraints to `skills/design-product-experience/SKILL.md`. Structural UX settles task model, critical journey, hierarchy, disclosure/defaults, states/recovery, adaptive behavior and accessibility before motion/polish.

## Validation depth

Use `.engineering/commands.json` as the canonical repository command map and `scripts/detect_ci_scope.py` as the project-owned risk selector. `auto` is normal:

- `LEAN` — documentation, governance and metadata plus cheap repository guards;
- `SCOPED` — contained app/UI/business-logic/test changes; format/debug compile/unit/Lint/debug assembly;
- `STRONG` — Harnex Consumer/Binder integration, PII/redaction/privacy/persistence/security boundaries, manifest, dependency/build, ProGuard/R8, AndroidTest or package/variant behavior;
- `FULL` — `dev -> main` promotion/release, validation selector/workflow changes, global Gradle/dependency inventory/toolchain changes, unknown executable scope or explicit full request.

Product depth does not mechanically select engineering validation depth. A product feature can be technically scoped; implementation-only CI/selector changes can require `FULL`.

If a narrow profile misses a deterministic failure in a materially affected component, strengthen selector/dependency mapping; do not make every PR full.

## Execution capability

Validation depth and execution location are separate. Required evidence is `AGENT_LOCAL`, `REMOTE_AUTOMATED` or `REAL_ENVIRONMENT`.

Automatable Gradle/Kotlin/Lint/R8/unit/AndroidTest-assembly/debug/release package gates become `REMOTE_AUTOMATED` when unavailable agent-local; do not delegate them to the user solely because local Android tooling is absent.

Classify failures before editing production code and fix the owning invariant. After repeated failed repairs with the same signature, change diagnostic strategy rather than applying another symptom patch.

Do not equate:

```text
unit/integration tests != smoke != E2E != physical-device evidence
```

## Pre-publication readiness

Before integration/release publication, use `skills/preflight-change/SKILL.md`: refresh exact head/base, review the complete diff, make affected durable docs current, select risks/gates/profile, choose affected E2E/fidelity/evidence mode, reuse equivalent evidence and route missing automatable work.

At `INTEGRATION`, required automatable evidence must pass while residual physical evidence may be explicitly `DEFERRED_TO_RELEASE`. `RELEASE_READY` additionally requires every applicable blocking real-environment confirmation.

## Pull requests

PRs should state:

- observable outcome and applicable `PRODUCT_NONE|LOCAL|FEATURE|STRATEGIC` depth;
- product intent/risks/success only to the proportional depth required;
- affected owners/invariants/contracts and product-experience impact where applicable;
- exact preflight HEAD/target base;
- selected `LEAN|SCOPED|STRONG|FULL` profile and concrete gates;
- `AGENT_LOCAL`, `REMOTE_AUTOMATED` and `REAL_ENVIRONMENT` evidence with truthful `PASS|FAIL|PENDING|N/A` status;
- affected E2E and durable docs;
- final integration/release readiness.

Promotion to `main` requires `FULL` automated validation on the exact candidate plus stronger real-environment evidence required by promoted claims.

Completed workstream plans are deleted by default after durable product/architecture/feature/ADR/test truth and release obligations move to their canonical owners.
