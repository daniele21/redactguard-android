# Document pipeline

Status: migration in progress
Owner: RedactGuard

The document domain is Android- and parser-independent. Stable source identities use one-based serialized IDs (`p0001-b0001`) derived deterministically from zero-based page/block indices. Raw URI values, PdfBox objects, Binder objects and model/runtime identities must not enter this domain.

Extracted page text is normalized into deterministic paragraph-like blocks. Unsupported control characters fail closed. Document names, extracted page text and normalized segment text are redacted from ordinary diagnostic strings.

`SourceRange` is half-open and is the canonical local coordinate primitive for exact reviewed/redacted occurrences.

## Android trust boundary

The Storage Access Framework picker requests only transient read access. Raw Android `Uri` values are stored in a process-local capability registry and never become workflow/domain state.

PdfBox parsing stays outside the UI/application process in an `android:isolatedProcess="true"` service. The app process opens the selected source and transfers only a read-only file descriptor. Binder/Messenger carries bounded completion metadata; extracted document text is streamed through a pipe rather than placed into Binder transactions.

Parsing is bounded to 200 pages, 1,000,000 returned characters and 30 seconds. Any truncation fails closed as `LIMIT_EXCEEDED`; a partial document is never silently sent to PII analysis. Coroutine cancellation owns parser unbinding and descriptor cleanup.

PDF redaction/export is a separate adapter slice because it depends on the converged Review/replacement domain. It must create a new output document and clean failed destinations without mutating the source.
