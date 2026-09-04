# Structured analysis protocol and chunk planning

Status: migration in progress
Owner: RedactGuard

RedactGuard owns the structured PII analysis protocol independently from the Harness transport/runtime SDK. Prompt version, definition-set version and JSON-schema version remain explicit and stable.

The model receives one fixed instruction plus a JSON data payload containing selected definitions and document segments. Definition text, examples and document text are treated as untrusted data and are JSON-escaped by one deterministic serializer. Ordinary diagnostics redact payload text.

Chunk planning is deterministic and stateless. It preserves normalized document blocks whenever they fit, and fragments oversized blocks only on Unicode code-point boundaries with stable `-fNNNN` identities. Host ceilings are represented inside the pure domain as `AnalysisLimits`; RG-6 is responsible for adapting public Consumer SDK capability limits into that app-owned type. The pure analysis domain does not import Binder or Harness SDK contracts.

A schema or fixed-overhead budget that cannot fit is rejected before document text is submitted. No silent truncation is permitted.
