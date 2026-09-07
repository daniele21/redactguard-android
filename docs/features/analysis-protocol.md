# Structured analysis protocol and chunk planning

Status: migration in progress
Owner: RedactGuard

RedactGuard owns the structured PII analysis protocol independently from the Harnex transport/runtime SDK. Prompt version, definition-set version and JSON-schema version remain explicit and stable.

## Prompt ownership and integrity

The analysis instruction has two RedactGuard-owned layers:

1. **editable guidance** — a user-visible preference with a robust built-in default;
2. **protected integrity rules** — mandatory instructions appended after editable guidance and never exposed as editable state.

The built-in prompt requires findings only for occurrences actually present in submitted segments. A finding must copy the supplied `typeId`, `segmentId` and surface literally; selected PII types that are absent are omitted rather than represented with placeholders such as `[Missing]`, `N/A`, `null` or equivalent sentinel values. When nothing is present, the expected result is the empty findings object.

The protected rules are defense in depth, not an authorization boundary. Final model output remains subject to RedactGuard's deterministic parser/validator; prompt text cannot make an invented type, segment or surface valid.

A custom prompt is an app-private durable RedactGuard preference. It is normalized and validated before storage, bounded to 4096 characters, never logged, and changes only future analyses. Absence of a stored custom value means "use the current built-in default", so future default improvements automatically reach users who never customized it.

At analysis start RedactGuard snapshots the current editable prompt into the product job. Every chunk for that job therefore uses the same effective instruction even if Settings changes while the job is running.

## Structured payload and trust boundary

The model receives the effective instruction plus a JSON data payload containing selected type IDs and document segments. Definition text/examples are supplied through the Consumer task-definition contract, while document text is treated as untrusted data and JSON-escaped by one deterministic serializer. Ordinary diagnostics redact prompt and payload contents.

Document text is never allowed to override the instruction contract. The prompt explicitly tells the model to treat submitted document segments as data rather than instructions, while RedactGuard's output validation independently enforces the canonical IDs/surfaces that can be accepted.

## Chunk planning and limits

Chunk planning is deterministic and stateless. It preserves normalized document blocks whenever they fit, and fragments oversized blocks only on Unicode code-point boundaries with stable `-fNNNN` identities. Host ceilings are represented inside the pure domain as `AnalysisLimits`; the Harnex integration boundary adapts public Consumer SDK capability limits into that app-owned type. The pure analysis domain does not import Binder or Harnex SDK contracts.

The planner budgets the **actual snapshotted effective instruction** for the job rather than assuming the built-in default. A longer custom prompt can therefore reduce available document capacity but can never silently exceed the Host input ceiling. A schema or fixed-overhead budget that cannot fit is rejected before document text is submitted. No silent truncation is permitted.
