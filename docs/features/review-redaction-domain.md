# Review and redaction planning domain

Status: migration in progress
Owner: RedactGuard

Human review decisions are product-owned and independent from reveal state. Each exact typed source occurrence is either `PENDING`, `ACCEPTED` or `IGNORED`; reveal remains presentation-only and is never persisted into the redaction decision.

The deterministic replacement planner fails closed when any decision is pending, an occurrence references an unknown definition or segment, the reviewed surface no longer matches the exact source range, duplicate occurrence identities exist, or accepted ranges overlap. Ignored overlaps do not block an accepted replacement.

Accepted occurrences are ordered by source position and receive deterministic placeholders such as `[EMAIL_1]`. Placeholder keys are ASCII, bounded, collision-safe and derived from definition labels. Replacements are applied from the highest source offset to the lowest so earlier source coordinates remain valid. The source document is not mutated by this pure domain layer; Android/PDF export is a separate adapter.

Sensitive occurrence surfaces and rendered segment text are redacted from ordinary diagnostic strings.

## Converged dependencies

This slice depends only on RedactGuard-owned `PiiTypeId`, `DocumentSegment`, `SourceOccurrence` and `SourceRange` contracts already present on `dev`. It does not import the Harness Consumer SDK, Binder types, PdfBox objects or Compose state. Finding validation will create these exact source occurrences before Review; the PDF adapter will consume the resulting replacement plan after all decisions are terminal.
