# RedactGuard — Coding Agent Guide

RedactGuard is a privacy-first Android document-protection product. It owns document/PII/review/redaction/export behavior; Harnex owns local-AI models/runtime administration. Sensitive content stays local by default: no silent cloud fallback or sensitive-value logging.

## Durable invariants

- Durable product mission/users/outcomes/principles live in `docs/product.md`; feature/UX/architecture owners implement them without duplicating product truth.
- Redaction/privacy policy belongs to the domain owner, not UI/adapters.
- Harnex/Consumer/Binder semantics stay explicit; Host absence/restart/denial never becomes fake success or an undeclared fallback.
- Protection-critical uncertainty fails closed; users remain the authority over accepted/rejected findings.
- Persisted/sensitive state has explicit lifecycle, bounds and cleanup.
- UI exposes actionable states without unnecessary sensitive data and follows canonical accessibility/adaptive/design owners.
- Emulator/two-APK evidence never implies production ARM64/GGUF/memory/thermal/OEM behavior.

## Ownership

| Change | Owner / proof |
| --- | --- |
| Product mission/users/outcomes/principles | `docs/product.md`, `.engineering/product.json` |
| Privacy/redaction semantics | domain privacy/redaction owners; ViewModels/adapters/domain tests |
| Persistence/platform/Harnex integration | infrastructure owners; lifecycle/recovery/contract tests |
| User task/recovery state | UI/ViewModel owners; Compose/journey evidence |
| Consumer/Binder boundary | Harnex integration surface; cross-process/two-APK evidence |
| Product experience | `design/*`; design-system/accessibility/journeys |

Follow the closest scoped `AGENTS.md`; extend the canonical owner before parallel state/policy.

## Read by task

| Task | Read now |
| --- | --- |
| Docs/copy | affected owner; `docs/README.md` if routing is unclear |
| Product capability/behavior/strategy shaping | `.engineering/product.json`, `docs/product.md`, `skills/shape-product-change/SKILL.md` |
| Behavior/bug/contract | `skills/structured-change/SKILL.md`, `skills/validate-change/SKILL.md`, relevant commands |
| Material UI | above + `skills/design-product-experience/SKILL.md`, relevant `design/*` |
| Integration/release | `skills/preflight-change/SKILL.md`, commands, `.engineering/e2e.json` |
| Missing deterministic remote gate | `skills/remote-preflight/SKILL.md` |
| Persistent work | `skills/plan-workstream/SKILL.md` + active plan; finalize via `skills/finalize-workstream/SKILL.md` |

## Product and delivery boundaries

Product depth, delivery stage and validation depth are independent.

- `PRODUCT_NONE/LOCAL`: no broad product ceremony; preserve settled intent and prove the local outcome.
- `PRODUCT_FEATURE/STRATEGIC`: establish user/problem/outcome, material value/usability/feasibility/viability risks, assumptions, non-goals and success before substantial implementation. Discovery may narrow, change or reject the requested solution.
- `ITERATION`: fast owner-local falsification; no publication ceremony after every edit.
- `INTEGRATION`: coherent user outcome ready for `dev`; current docs, exact candidate/base, required automated gates and affected E2E. Material UI/UX journeys use `FULL_MEDIA`; residual physical proof is `DEFERRED_TO_RELEASE`.
- `RELEASE`: `FULL` plus release-critical package/E2E and applicable blocking real-environment confirmation.

`SHIPPED` proves delivery, not product impact. Define post-release learning only when real use must answer something material; telemetry is not mandatory.

## Evidence, context and failure discipline

Use the cheapest sufficient declared environment. `.engineering/documentation-policy.json` owns context routes; use `--route product` for shaping and `--route bug` for implementation. Routes never authorize omitting relevant source/instructions.

Privacy/security, persistence, Harnex/Binder, manifest/package/R8 and unknown/global scope legitimately escalate. Missing Android tooling is `REMOTE_AUTOMATED`, not user-run Gradle. On failure classify before editing; after two failed repairs with the same signature, change diagnostic strategy and gather new evidence.

Before integration update affected canonical docs. Transfer durable truth and deferred release obligations before deleting completed plans. Never suppress legitimate privacy/security/contract tests, hide failed/pending gates, leak sensitive data or downgrade evidence to obtain PASS.
