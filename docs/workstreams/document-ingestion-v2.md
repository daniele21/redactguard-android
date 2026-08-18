# Document Ingestion V2

Status: active
Owner: redactguard-android
Read when: implementing or coordinating text-first document ingestion for RedactGuard

## Goal

Make RedactGuard reliably ingest two text-bearing input classes — text PDFs and pasted plain text — into the same canonical `DocumentSegment` pipeline, while keeping OCR explicitly out of scope and preserving privacy, bounded-resource and diagnosable-failure guarantees.

## Non-goals

- OCR, scanned/image-only PDF recognition or VLM extraction;
- layout-faithful PDF reconstruction;
- DOCX/HTML/email ingestion;
- cloud parsing or remote document upload;
- replacing Harness analysis, PII detection, Review or redaction semantics.

## Invariants

- document content remains local and process-local except for the existing local Harness IPC contract;
- PDF and pasted-text ingestion converge before analysis on the same canonical segment contract;
- pasted text is never persisted through `SavedStateHandle`, preferences, database or application files;
- PDF parsing remains isolated and bounded;
- a known parser failure is never relabeled as malformed input without evidence that the PDF structure is actually invalid;
- image-only PDFs remain an explicit unsupported-input outcome until a later OCR/VLM workstream;
- existing analysis atomicity, review identity and fail-closed export behavior remain unchanged.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| DI-0 | Baseline current ingestion/failure boundaries and define the target convergence point | `docs/workstreams/document-ingestion-v2.md` | — | yes | DONE |
| DI-1 | Introduce source-neutral text-page segmentation and pasted-text extractor contract | `domain/document/*`, new text-ingestion source/tests | DI-0 | yes | ACTIVE |
| DI-2 | Add pasted-text product flow from UI to canonical document | `MainActivity.kt`, import UI, `RedactGuardProductViewModel.kt` | DI-1 | yes | BLOCKED |
| DI-3 | Harden text-PDF parsing classification and preserve parser diagnostics without OCR | PDF reader/service/extractor + import failure mapping/tests | DI-0 | yes | READY |
| DI-4 | Add source-specific stable failures and user recovery copy for pasted text | failure domain/projector/mapping tests | DI-1 | yes | BLOCKED |
| DI-5 | Integration tests for PDF/text convergence, resource limits and lifecycle cleanup | JVM tests and architecture/quality tests | DI-1, DI-2, DI-3, DI-4 | no | BLOCKED |
| DI-6 | Repository validation and physical smoke evidence with representative text PDFs and pasted text | CI/evidence only | DI-5 | no | BLOCKED |
| DI-7 | Transfer durable behavior to canonical docs and close the workstream | `docs/architecture.md`, feature docs, `docs/current-state.md` | DI-6 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

Parallel work must have explicit non-conflicting ownership/write boundaries or a defined integration point.

## Current executable slice

`DI-1` and `DI-3` may execute in parallel.

Acceptance for `DI-1`:

- PDF-extracted page text and pasted text can be normalized by one source-neutral segmenter;
- pasted text produces deterministic segment IDs and a one-page canonical document;
- blank and over-limit pasted text fail deterministically without persistence.

Validation for `DI-1`:

- `./gradlew :app:testDebugUnitTest --tests '*document*'`

Acceptance for `DI-3`:

- generic `IOException` no longer means `MALFORMED_PDF` by default;
- encrypted, image-only, empty, limit-exceeded and genuinely malformed inputs remain distinct where truthfully identified;
- unexpected PDF parser failures preserve safe parser-stage/type diagnostics while user-facing identity remains product-owned;
- parser descriptors/service bindings remain cleaned up on success, failure and cancellation.

Validation for `DI-3`:

- `./gradlew :app:testDebugUnitTest --tests '*ImportFailure*' --tests '*AndroidDocumentExtractor*'`

## Integration points

- `ExtractedDocument` remains the single application-facing document result consumed by definitions/analysis/review/export.
- `DocumentSegment` remains the canonical downstream unit; no source-specific PDF/text types may escape into Harness analysis.
- UI source selection integrates only through ViewModel ingestion entry points; pasted-text draft state remains Compose-local until submission.
- PDF diagnostics integrate with the existing failure-diagnostics workstream through stable product failures rather than raw PDFBox exception classes.

## Durable documentation destinations

- `docs/architecture.md`: source-neutral ingestion boundary and PDF isolation ownership;
- `docs/features/document-ingestion.md`: supported inputs, limits and image-only behavior;
- `docs/workstreams/failure-diagnostics-hardening.md`: only if the canonical PDF failure registry changes materially;
- tests/contracts: deterministic text segmentation, failure mapping and convergence behavior.

## Completion

The workstream is complete only when pasted text and representative text PDFs reach the same analysis contract, image-only PDFs fail explicitly without OCR, failure/resource behavior is tested, repository validation is green and durable docs agree. Then update `docs/current-state.md` and delete this file by default.