# RedactGuard PII quality policy v1

Status: migrated, pre-registered, not yet a model support claim

The active synthetic corpus remains `ombra-pii-synthetic-v2` during repository extraction so its identity is byte-for-byte comparable with the already registered baseline. Repository/product renaming does not authorize changing corpus content, hash or acceptance thresholds.

Frozen corpus SHA-256: `a04f79dec42ee4208e4db27512664cc20f66cc863fd80ae4fcdc1019a2f37a5f`.

Acceptance thresholds remain:

- aggregate precision >= 0.90
- aggregate recall >= 0.98
- aggregate F1 >= 0.94
- per-category precision >= 0.80
- per-category recall >= 0.90
- per-category F1 >= 0.85
- structured completion >= 0.98
- invalid finding rate <= 0.02
- invalid result rate = 0.00

Any corpus-content or threshold change requires an explicit new version and identity. The migration itself must not tune the gate to observed model results.
