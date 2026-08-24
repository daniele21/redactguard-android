# RedactGuard key reference views

Keep this directory intentionally small. These references describe the product states whose hierarchy and semantics should remain stable across implementation changes; they are not a gallery of generated screenshots.

## Import and input

The entry surface must make the privacy task obvious and keep one primary action. `Import PDF` is primary; `Paste text` is secondary. Supported-input constraints are contextual. Runtime implementation terminology is absent from the default view.

Key states: default, importing, permission/source failure, image-only PDF recovery.

## Analysis and review

Analysis communicates truthful phases and offers cancellation without inventing percentage completion. Review keeps the current occurrence and its redact/ignore decision dominant, preserves hidden-by-default sensitive values and shows review progress without overwhelming the finding itself.

Key states: runtime unavailable, analysis running, cancellation, findings available, no findings, export disabled/enabled.

## Error and recovery

The error surface leads with the user-understandable cause and the correct recovery action. Stable code, stage and operation ID remain behind technical-detail disclosure. User content, prompts, findings and raw Binder/parser payloads never appear in diagnostics.

Key states: classified input failure, local-AI dependency failure, export destination/write failure, unknown internal fallback.

## Evidence policy

When RTA-9 introduces screenshot or visual-regression evidence, retain only the minimal stable reference set justified by regression risk. Generated run artifacts remain bounded CI/evidence artifacts with build/source identity.
