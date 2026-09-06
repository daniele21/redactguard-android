# RedactGuard — Coding Agent Guide

RedactGuard is a privacy-first Android document-protection app using local analysis and an optional Harnex boundary. Sensitive content stays local by default; never add silent cloud fallback or sensitive-value logging.

## Durable invariants

- Redaction/privacy policy belongs to the domain owner, not UI or adapters.
- Harnex/Consumer/Binder semantics stay explicit; Host absence/restart never becomes fake success.
- Persisted and sensitive state has explicit lifecycle, bounds and cleanup.
- UI exposes actionable states without leaking unnecessary sensitive data and follows hierarchy, progressive disclosure, accessibility/adaptive behavior and canonical design owners.
- Emulator evidence never implies production ARM64/GGUF/memory/thermal/OEM behavior.
- Build/package identity and release evidence remain reproducible and privacy-safe.

## Ownership

| Change | Owner | Direct consumers / proof |
| --- | --- | --- |
| Privacy/redaction semantics | domain/privacy/redaction owners | ViewModels, adapters, domain tests |
| Persistence/platform/Harnex integration | infrastructure owners | lifecycle/recovery/contract tests |
| User task/recovery state | UI/ViewModel owners | Compose/UI journey evidence |
| Consumer/Binder boundary | Harnex integration surface | cross-process/two-APK evidence |
| Product experience | `design/*` | design-system/accessibility/journeys |

Follow the closest scoped `AGENTS.md`; extend the canonical owner before adding parallel state or policy.

## Read by task

| Task | Read now |
| --- | --- |
| Pure docs/copy | affected source/links; `docs/README.md` only if ownership is unclear |
| Behavior/bug/contract | `skills/structured-change/SKILL.md`, `skills/validate-change/SKILL.md`, relevant commands |
| Material UI | above + `skills/design-product-experience/SKILL.md` and relevant `design/*` |
| Integration/release | `skills/preflight-change/SKILL.md`, commands, affected `.engineering/e2e.json` |
| Missing deterministic remote gate | `skills/remote-preflight/SKILL.md` |
| Persistent multi-session work | `skills/plan-workstream/SKILL.md` + active plan; finalize with `skills/finalize-workstream/SKILL.md` |

Do not load every workstream or run release-grade Android validation for every edit.

## Delivery boundaries

- **ITERATION**: fast owner-local falsification; no exact-head/full-diff/docs/publication ceremony after every edit.
- **INTEGRATION**: coherent user outcome ready for `dev`; current affected docs, exact candidate/base, required automated gates and affected critical E2E. Material UI/UX integration journeys require `FULL_MEDIA`. Residual physical confirmation is `DEFERRED_TO_RELEASE`.
- **RELEASE**: `FULL` plus release-critical package/E2E and every applicable required real-environment confirmation.

Profiles summarize selected gates, not fixed suites. Privacy/security, persistence, Harnex/Binder, manifest/package/R8 and unknown/global scope legitimately escalate. Missing local Android tooling means `REMOTE_AUTOMATED`, not user-run Gradle.

## E2E, context and failure discipline

Use the cheapest sufficient declared environment. `protect-text-document`/`protect-text-pdf` normally need screenshots; `recover-local-ai` and background lifecycle sequence use `FULL_MEDIA`; Binder roundtrip is assertion-oriented. Emulator/two-APK proof remains narrower than production ARM64/model/device evidence.

`.engineering/documentation-policy.json` owns context routes; use `python3 scripts/verify_agent_context.py --route bug --format json`, optionally with `--path`/`--workstream`. Routes estimate context cost; they never authorize omitting relevant code/instructions.

For meaningful work state observable outcome, owner, invariants and proof. Classify failures before editing; each failed repair needs a falsifiable hypothesis. After two failed repairs with the same signature, change diagnostic strategy and gather new evidence before a third. On resume refresh head/tree/base and treat checkpoint evidence as pointers, not source truth.

Before integration update affected canonical docs. Transfer durable truth and deferred release obligations before deleting completed plans. Never suppress legitimate privacy/security/contract tests, hide failed/pending gates or downgrade evidence to obtain PASS.
