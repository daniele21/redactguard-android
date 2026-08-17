# Current state

Status: active
Last reviewed: 2026-08-17
Canonical scope: repository.current-state

## Product state

RedactGuard is being extracted from the existing OMBRA implementation currently integrated in `daniele21/android-local-llm-harness`.

The target is an independent Android application repository that owns PDF/PII/review/redaction/export product behavior and consumes a versioned Harness Consumer Android SDK over Binder at runtime.

## Repository state

- repository created and initial README committed on `main`;
- `dev` branch created for active integration work;
- canonical cross-repository migration plan added at `docs/workstreams/ombra-to-redactguard-migration.md`;
- Android project/toolchain/CI bootstrap is not yet complete;
- no migrated product source is canonical here yet;
- no Harness source dependency is allowed as the final integration state.

## Known-good source baseline

Until cross-repository cutover, the current OMBRA implementation in `android-local-llm-harness` remains the behavioral reference/rollback source for:

- PII definitions and validation;
- PDF import/extraction/export;
- chunk planning and structured result validation;
- Review and redaction policy;
- Binder Consumer API composition;
- synthetic corpus and quality policy.

New behavior should not diverge silently during migration.

## Active execution wave

The following workstreams can start immediately and should be developed independently where practical:

- RG-0 — repository/bootstrap and Android engineering contract;
- HSDK-1 — publishable Harness Consumer Android SDK;
- HHOST-1 — RedactGuard host identity/authorization;
- RG-2 — pure product-domain migration;
- RG-3 — PDF pipeline migration;
- RG-4 — product UI/Review migration;
- RG-5 — quality corpus/policy migration.

The first serialized dependency begins at RG-6, when RedactGuard must consume the real published SDK.

## Critical path

```text
HSDK-1 + HHOST-1 + RG-1/RG-2
            |
            v
          RG-6
            |
            v
          INT-1
            |
            v
          E2E-1
            |
            v
          E2E-2
            |
            v
          CUT-1
```

## Current blockers

- no externally consumable versioned Harness Consumer Android SDK exists yet;
- RedactGuard Android shell/toolchain has not yet been bootstrapped;
- new RedactGuard package/application identities are not yet authorized by the Harness host;
- physical cross-repository evidence cannot start until real artifacts from both repositories are buildable.

## Next state transition

The repository moves from bootstrap to active product migration when RG-0 produces a buildable Android shell and at least RG-2/RG-3/RG-4 can land against fake/local application ports while HSDK-1 proceeds independently.
