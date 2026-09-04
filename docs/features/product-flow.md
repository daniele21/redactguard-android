# End-to-end RedactGuard product flow

Status: repository integration in progress
Owner: RedactGuard

RedactGuard owns the complete product workflow while Harness remains an external Local AI provider.

## Flow

1. `MainActivity` requests a PDF through Android Storage Access Framework `OpenDocument`.
2. The URI is registered as a process-local `DocumentSourceRef`; the isolated parser reads it and returns normalized `DocumentSegment` values. The source capability is released after extraction.
3. The user selects built-in PII definitions and may add bounded custom definitions. Definition selection is product-owned and process-local.
4. RedactGuard connects explicitly to the configured Harness package/service through the published `consumer-android` SDK. Activity resume retries the explicit connection, allowing recovery after Harness is installed or restarted without reinstalling RedactGuard.
5. `SequentialDocumentAnalyzer` prepares the `document-pii-detection` use case, obtains app-owned limits, creates the deterministic chunk plan and runs chunks sequentially. Harness selects the allowed model/preset; RedactGuard exposes no model selector or runtime tuning.
6. Every model response is parsed as strict structured JSON and all findings are validated against the complete chunk plan and canonical source. No partial finding list reaches Review when a later chunk fails.
7. Review is hidden by default. Only the exact occurrence explicitly revealed by the user may be present as clear text in presentation state; navigation, decision changes, export and task reset clear reveal state.
8. After every occurrence is accepted or ignored, `RedactionPlanner` creates the deterministic replacement plan. Accepted occurrences receive placeholders; ignored surfaces remain unchanged.
9. Android `CreateDocument` provides the explicit export destination. RedactGuard writes a newly generated normalized PDF, not a copy of source-PDF objects. A failed write is fail-closed with best-effort deletion of partial output.
10. The success screen reports `PDF protetto creato`; correctness is proved separately by the physical two-APK evidence run, which reopens the exported PDF independently.

## Sensitive state lifecycle

Document text, normalized segments, validated finding surfaces, review decisions and reveal state are private fields of the process-local product controller. They are not stored in `SavedStateHandle`, preferences, a database or application files. Compose uses ordinary `remember` only for transient dialog visibility/input and never `rememberSaveable` for sensitive product data.

`newDocument()` and ViewModel teardown clear process-local sensitive state. Process recreation therefore starts from a fresh import state rather than restoring the previous document/review task.

## Cross-repository boundary

Harness/Binder contract types are confined to `infrastructure/localai`. The application layer sees only app-owned runtime state, `AnalysisRuntimePort` and product-domain result types. PDF parsing/export remain RedactGuard infrastructure; llama.cpp, GGUF/model lifecycle, host scheduling and runtime telemetry remain Harness responsibilities.

Repository acceptance for this flow requires the normal `Validate` workflow to pass formatting, JVM tests, Android Lint and debug assembly on the same user-authored head after canonical formatting. Repository CI cannot prove the final same-signer physical Binder path or independently inspect an exported device PDF; those claims remain gated by `docs/evidence/physical-two-apk.md`.
