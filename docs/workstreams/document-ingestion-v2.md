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
| DI-1 | Introduce source-neutral text-page segmentation and pasted-text extractor contract | `domain/document/*`, new text-ingestion source/tests | DI-0 | yes | DONE |
| DI-2 | Add pasted-text product flow from UI to canonical document | `MainActivity.kt`, import UI, `RedactGuardProductViewModel.kt` | DI-1 | yes | DONE |
| DI-3 | Harden text-PDF parsing classification and preserve parser diagnostics without OCR | PDF reader/service/extractor + import failure mapping/tests | DI-0 | yes | DONE |
| DI-4 | Add source-specific stable failures and user recovery copy for pasted text | failure domain/projector/mapping tests | DI-1 | yes | DONE |
| DI-5 | Integration tests for PDF/text convergence, resource limits and lifecycle cleanup | JVM tests and architecture/quality tests | DI-1, DI-2, DI-3, DI-4 | no | DONE |
| DI-6 | Repository validation and physical smoke evidence with representative text PDFs and pasted text | CI/evidence only | DI-5 | no | ACTIVE |
| DI-7 | Transfer durable behavior to canonical docs and close the workstream | `docs/architecture.md`, feature docs, `docs/current-state.md` | DI-6 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

Parallel work must have explicit non-conflicting ownership/write boundaries or a defined integration point.

## Repository validation evidence

PR #49 reached a complete green repository validation on branch head `8d205d7c47e3b7c8392bc78b107c31f0f7de3c27`, workflow run `32132232998`:

```text
Spotless                         PASS
Failure-contract guard           PASS
Compile app Kotlin               PASS
Compile JVM unit tests           PASS
Run JVM unit tests               PASS
Android Lint                     PASS
Assemble debug APK               PASS
Assemble minified release APK    PASS
```

This proves the source-neutral ingestion contract, failure mappings and existing application contracts are repository-valid. It does **not** prove Android isolated-process PDF parsing against real device/content-provider boundaries; that remains DI-6.

## Current executable slice

`DI-6` — physical smoke evidence.

Required scenarios:

1. pasted text with representative PII reaches Definitions and Harness analysis without invoking the PDF parser;
2. a simple text-bearing PDF reaches the same canonical analysis path;
3. a multi-page text-bearing PDF reaches the same canonical analysis path without truncation or partial-result exposure;
4. an image-only PDF fails explicitly as `RG-PDF-008 / IMAGE_ONLY_PDF`; OCR/VLM is not invoked;
5. if a text-bearing PDF fails unexpectedly, it must surface `RG-PDF-005 / PARSER_FAILED` unless a more specific cause is truthfully known, and technical details must expose safe parser `Step`/`Errore parser` identity plus operation ID;
6. returning to a new document clears task-local pasted/PDF text and review state;
7. Harness analysis/review/export behavior remains unchanged after either input route.

Physical evidence must record exact RedactGuard APK/head, Harness APK/head, device/API identity and observed stable failure code for any negative case. User documents containing real PII must **not** be committed as repository fixtures.

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

The workstream is complete only when pasted text and representative text PDFs reach the same analysis contract on a real device, image-only PDFs fail explicitly without OCR, failure/resource behavior is tested, repository validation is green and durable docs agree. Then update `docs/current-state.md` and delete this file by default.
