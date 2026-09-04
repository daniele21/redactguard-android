# Sequential document analysis

Status: integration in progress
Owner: RedactGuard

RedactGuard owns the document-analysis orchestration above the Local AI runtime port. Harness prepares the allowed use case and executes one structured generation at a time; it does not own product chunking, result parsing or source validation.

The analyzer prepares execution limits, creates a deterministic chunk plan and submits chunks sequentially. Raw model output is treated as untrusted data. Each chunk must parse as the exact versioned structured result, but no finding is exposed to Review while later chunks remain outstanding.

After every planned chunk completes, RedactGuard validates the combined untrusted findings against the full chunk plan, canonical document segments and selected PII definitions. This provides one atomic success boundary across chunks and allows overlap/source consistency checks to run globally. If any chunk fails, returns malformed JSON or produces invalid findings, the operation fails closed and no partial review result is emitted.

Cancellation propagates through the app-owned runtime port, the operation is removed, and runtime/session cleanup is requested. Runtime failure codes are mapped into product-owned document-analysis failures; Binder and Harness types remain outside this domain layer.

Repository acceptance requires the normal `Validate` workflow to pass unit tests, Android Lint and debug assembly on the same user-authored head after canonical Spotless formatting has been applied.
