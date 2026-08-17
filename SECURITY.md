# Security

RedactGuard is local-first and must not silently upload documents, prompts, findings or generated content. Sensitive document state is process-local by default and must not enter logs, analytics, SavedState, routes or ordinary crash telemetry.

The Local AI Host trust boundary is protected by the Harness signature/package/application authorization model. RedactGuard must not weaken or bypass that model.

Do not commit signing keys, credentials, production documents, model binaries or user-derived fixtures.
