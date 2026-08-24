# Contributing

RedactGuard uses `dev` as the integration branch and focused workstream/slice branches for parallel changes. `main` is the release/canonical branch when a release is promoted.

Before editing:

1. read `AGENTS.md` and `docs/current-state.md`;
2. find the canonical owner in architecture/features/design/operating contract;
3. read the relevant active workstream when the change belongs to one;
4. inspect direct consumers and tests before modifying shared behavior.

## Change discipline

Keep ownership explicit and prefer the smallest change that preserves required invariants. Do not add dependencies, abstraction layers, caches, services, UI component families or persistence without a concrete need.

Preserve the product boundaries: RedactGuard owns document/PII/review/export behavior; Harness owns local-model runtime and control-plane behavior exposed through the published Consumer SDK.

Sensitive document text/findings/review state remain process-local by default, diagnostics remain privacy-safe, and there is no silent cloud fallback.

## UX/UI changes

When `product-ui` is adopted, read `design/ux-contract.json` and `design/brand-kit.json`. Structural UX must settle user outcome, task model, critical journey, hierarchy, disclosure/defaults, states/recovery, adaptive behavior and accessibility before design-system/motion/polish work.

## Validation

Use `.engineering/commands.json` as the canonical repository command map. While iterating, run the narrowest deterministic gate that can falsify the change quickly; before finalization expand to the correct repository/integration/E2E/device evidence for the claim.

Do not equate:

```text
unit/integration tests != smoke != E2E != physical-device evidence
```

If required evidence cannot run, report it as pending rather than passed.

## Pull requests

PRs should be bounded to one workstream slice/owner where practical and state:

- what changed and why;
- affected invariants/contracts;
- product-experience impact when applicable;
- build/runtime/artifact impact when applicable;
- exact validation executed;
- required evidence still pending;
- durable docs/design contracts changed or why none are needed.

Completed workstream plans are deleted by default after durable current behavior has moved to architecture/features/ADR/tests and `docs/current-state.md` has been updated.
