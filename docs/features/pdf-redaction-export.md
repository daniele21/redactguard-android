# Redacted PDF export

Status: migration in progress
Owner: RedactGuard

RedactGuard exports from an already validated `RedactionPlan`. The Android exporter does not decide which occurrences are accepted, ignored or conflicting and does not reconstruct findings. Those invariants belong to the product domain.

The output is a newly generated normalized-layout PDF assembled from rendered segment text. Source-PDF objects, annotations, metadata and embedded resources are not copied into the destination. Pages preserve page order and block order, while accepted source surfaces have already been replaced by deterministic placeholders.

The destination is the explicit SAF `Uri` selected by the application. A failed write is fail-closed and triggers best-effort deletion of the partial destination. Successful export requires a positive byte count. Page count and document-character bounds are explicit.

Physical evidence must independently reopen the exported PDF and verify that accepted synthetic PII is absent and ignored content remains present; successful writer return alone is not sufficient evidence.
