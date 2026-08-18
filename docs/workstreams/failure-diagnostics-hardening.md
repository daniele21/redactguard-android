# RedactGuard Failure Diagnostics Hardening Plan

Status: active
Document type: workstream
Owner: redactguard-android
Canonical scope: repository.failure-diagnostics-hardening
Last reviewed: 2026-08-18

## Purpose

RedactGuard must preserve and expose the reason for failures across the complete product lifecycle. The current PDF import flow classifies detailed extraction failures at the owning boundary but collapses multiple known causes into a generic product error before they reach the user. This loses diagnostic information and does not satisfy the failure, observability and recovery expectations adopted from `repo-template-sw`.

This workstream hardens failure handling so that every meaningful operation is identifiable, classified, observable, privacy-safe and recoverable, while keeping technical diagnostics progressively disclosed rather than dominating the normal product flow.

## Problem statement

The current document extraction boundary already distinguishes failures such as:

- `SOURCE_NOT_FOUND`
- `SOURCE_UNREADABLE`
- `ENCRYPTED_PDF`
- `MALFORMED_PDF`
- `PARSER_FAILED`
- `LIMIT_EXCEEDED`
- `EMPTY_PDF`
- `IMAGE_ONLY_PDF`

However, the product projection currently maps several distinct causes to `IMPORT_UNSUPPORTED`, producing a generic message such as “Il file è cifrato, non valido, troppo grande o non contiene testo utilizzabile.”

The same anti-pattern must be prevented across analysis, review, export and Harness connectivity: a known failure cause must not be silently collapsed into an indistinguishable generic state unless the lower boundary genuinely cannot classify it further.

## Governing invariants

This workstream adopts the following invariants.

1. **Classify at the owning boundary.** The component that can truthfully identify a failure owns its canonical classification.
2. **Preserve cause identity end to end.** Known causes must survive adapter, domain, ViewModel and UI projection boundaries.
3. **Stable machine-readable codes.** User-facing failures have stable product-owned codes independent of exception class names.
4. **Actionable user-facing errors.** When known, the UI explains what failed, why or which constraint was violated, and what the user can do next.
5. **Progressive disclosure.** Normal users see concise actionable copy; technical identifiers and diagnostics are available in an expandable/detail surface.
6. **No sensitive payloads in diagnostics.** Document text, extracted PII, findings, prompts and user content are never logged by default.
7. **Every long-running operation has identity.** Import, analysis and export failures can be correlated with an operation ID and build/source identity where relevant.
8. **Retry semantics are explicit.** Each failure declares whether retrying the same operation can reasonably succeed and what recovery action is required.
9. **Unknown is exceptional, not a bucket.** Generic/unknown failures remain possible for truly unexpected cases but must be observable and tested as a distinct fallback.
10. **Failure handling is part of Definition of Done.** New critical paths are incomplete until their failure classification, user projection, diagnostics and tests exist.

## Target failure contract

Introduce a product-owned failure model that does not leak Android, Binder, PDFBox, Harness SDK or exception implementation details into UI state.

Conceptual shape:

```text
ProductFailure
├── code                  stable product code
├── stage                 IMPORT / PARSE / ANALYSIS / REVIEW / EXPORT / CONNECTION
├── category              input / dependency / limit / protocol / integrity / internal
├── cause                 product-owned cause enum
├── userTitle
├── userExplanation
├── recoveryAction
├── retryable
├── operationId           when applicable
└── diagnosticContext     privacy-safe metadata only
```

The exact Kotlin shape may differ if a smaller representation preserves the same invariants. Avoid speculative abstractions and keep ownership local to the product/domain boundary.

## Initial error-code registry

The first implementation should cover at least the current known causes.

### PDF/import

| Code | Cause | User meaning | Retry |
|---|---|---|---|
| `RG-PDF-001` | `SOURCE_NOT_FOUND` | Selected source is no longer available | Re-select file |
| `RG-PDF-002` | `SOURCE_UNREADABLE` | App cannot read the selected source | Re-select / fix permission |
| `RG-PDF-003` | `ENCRYPTED_PDF` | PDF is password-protected/encrypted | Remove protection and retry |
| `RG-PDF-004` | `MALFORMED_PDF` | PDF structure is invalid/corrupt | Use a valid copy |
| `RG-PDF-005` | `PARSER_FAILED` | Parser failed unexpectedly | Retry or use another copy; diagnostics available |
| `RG-PDF-006` | `LIMIT_EXCEEDED` | Document exceeds an explicit parser bound | Use a smaller/supported document |
| `RG-PDF-007` | `EMPTY_PDF` | PDF has no pages/content to analyze | Use another document |
| `RG-PDF-008` | `IMAGE_ONLY_PDF` | PDF has no usable text layer | OCR not currently supported; use text PDF |

### Harness / analysis

The exact registry must be derived from existing `DocumentAnalysisFailureCode`, Consumer SDK outcomes and connection states, but should at minimum preserve distinct causes for:

- Host unavailable/not installed/disconnected;
- permission denied;
- protocol/capability incompatibility;
- request/plan rejection;
- invalid structured result / invalid JSON;
- invalid findings;
- chunk failure;
- cancellation;
- timeout when applicable;
- unexpected internal failure.

Suggested initial namespace: `RG-AI-001...`.

### Review/export

Preserve distinct causes for at least:

- invalid review/redaction plan;
- destination unavailable/unwritable;
- write failure;
- partial-output cleanup failure if this can occur;
- independent validation/reopen failure when introduced.

Suggested initial namespace: `RG-REV-*` and `RG-EXP-*`.

## Architecture flow

The desired flow is:

```text
LOW-LEVEL FAILURE
      ↓
classified at the owning boundary
      ↓
product-owned canonical cause/code
      ↓
preserved across layers
      ↓
┌─────────────────────────────┐
│ user-facing error projection│
└─────────────────────────────┘
      +
┌─────────────────────────────┐
│ structured diagnostic event │
└─────────────────────────────┘
```

No boundary may replace a more-specific known cause with a less-specific cause merely for UI convenience.

## Privacy-safe diagnostics

Diagnostics must help answer what failed and why without recording user content.

Allowed metadata should be explicitly whitelisted, for example:

- operation ID;
- stable error code;
- stage;
- duration;
- page count;
- bounded document size metadata when needed;
- retryable flag;
- parser error class/type only when normalized and safe;
- Harness protocol/capability version;
- app version;
- build ID/source revision;
- device/API/ABI class when useful for device-specific failures.

Forbidden by default:

- PDF text;
- filenames when they may contain PII, unless explicitly sanitized;
- prompts;
- model output;
- extracted findings;
- PII labels/values tied to document content;
- raw Binder payloads;
- stack traces shown directly to end users.

If local structured logs are introduced, retention and cleanup must be bounded and documented.

## UX contract

Every critical error screen must answer:

1. What happened?
2. Why, when RedactGuard knows the cause?
3. Did RedactGuard preserve/modify the document?
4. What should the user do next?
5. Can the operation be retried?
6. How can technical details be inspected without exposing them by default?

Example image-only PDF state:

```text
PDF senza testo estraibile

Questo PDF non contiene testo che RedactGuard riesce ad analizzare.
Potrebbe essere composto da immagini o scansioni.

RedactGuard al momento non esegue OCR.

[Nuovo documento]

Dettagli tecnici
RG-PDF-008 · IMAGE_ONLY_PDF
Fase: PDF extraction
```

The wording is illustrative; final microcopy must remain concise and consistent with the product design system.

## Work breakdown

### FD-0 — Baseline and failure inventory

**Goal:** create a complete current-state map before modifying contracts.

Tasks:

- enumerate all failure enums/exceptions/outcomes in document import, Harness connection, analysis, review and export;
- trace each cause from origin to final UI projection;
- identify every many-to-one mapping that loses useful cause information;
- identify raw `Throwable`, generic `IOException`, catch-all and silent fallback boundaries;
- record current retry semantics and cleanup behavior;
- classify which causes are safe to expose to users and which remain diagnostic-only;
- produce the canonical failure registry used by subsequent tasks.

**Output:** failure inventory table in this document or a dedicated durable error-contract document if it becomes large.

**Dependencies:** none.

**Parallelism:** can run in parallel with FD-1 design work after the initial PDF/import mapping is confirmed.

### FD-1 — Product failure contract and stable code registry

**Goal:** introduce the minimal product-owned failure representation.

Tasks:

- define stable product codes and stage/cause taxonomy;
- define retry/recovery semantics;
- define optional privacy-safe diagnostic metadata;
- ensure domain/UI contracts do not expose PDFBox/Binder/SDK exception types;
- define a single owner for code-to-copy/recovery projection;
- document compatibility expectations for codes once shipped.

**Acceptance criteria:**

- every currently known critical failure has one canonical product cause/code;
- no code depends on exception class name for public identity;
- unknown/internal fallback is explicit;
- compilation/test coverage establishes exhaustive mapping.

**Dependencies:** FD-0 inventory.

**Parallelism:** blocks FD-2/FD-3 implementation but UX copy drafting and telemetry design may proceed in parallel once the registry stabilizes.

### FD-2 — Preserve PDF/import failure identity end to end

**Goal:** remove the current `IMPORT_UNSUPPORTED` information collapse.

Tasks:

- map each `DocumentExtractionFailureCode` to a distinct product failure;
- preserve relevant bounded parser context when safe;
- project distinct actionable user messages;
- expose stable technical code through progressive disclosure;
- make limit messages reflect actual configured limits rather than generic “too large” wording;
- distinguish image-only from empty PDF;
- distinguish encrypted from malformed/corrupt PDF;
- retain parser-unexpected as a separate diagnostic path.

**Acceptance criteria:**

- `IMAGE_ONLY_PDF` can never render the encrypted/malformed/too-large omnibus message;
- each extraction code has a deterministic UI projection;
- no raw parser exception reaches UI;
- all paths preserve cleanup of descriptors/service binding/source registry.

**Dependencies:** FD-1.

**Parallelism:** can run in parallel with FD-3 and FD-4.

### FD-3 — Preserve Harness/analysis failure identity

**Goal:** ensure the app can explain analysis failures instead of collapsing protocol/runtime/result errors.

Tasks:

- audit `DocumentAnalysisFailureCode`, Consumer SDK errors and Binder connection outcomes;
- separate Host unavailable, Host death/disconnect, permission denial, incompatibility, invalid JSON/structured result, invalid findings, chunk failure, cancellation and timeout where distinguishable;
- preserve atomic-result invariant: failure details must not expose partial findings;
- ensure retry targets differ by cause;
- attach operation identity to asynchronous analysis failures;
- ensure cancellation is represented as a lifecycle outcome and not reported as an unexplained error when user initiated.

**Acceptance criteria:**

- every known analysis/connection cause maps to a stable product failure;
- retry/recovery action is cause-specific;
- no Harness/Binder implementation type leaks to UI;
- no partial PII result is emitted or logged on failure.

**Dependencies:** FD-1.

**Parallelism:** parallel with FD-2 and FD-4.

### FD-4 — Review/export failure hardening

**Goal:** classify and recover from failures in the remaining critical user journey.

Tasks:

- distinguish invalid review/redaction plan from export destination/write failures;
- preserve write-failure cleanup evidence;
- classify SAF permission/destination errors separately when truthfully distinguishable;
- define behavior when partial output cleanup itself fails;
- retain operation identity and relevant destination metadata without logging sensitive path/name data;
- align retry targets with whether a new analysis or merely a new destination is needed.

**Acceptance criteria:**

- the user is never told to rerun analysis for a failure that only requires selecting a new export destination;
- failed export cannot be mistaken for a valid generated PDF;
- failure/cleanup paths are tested.

**Dependencies:** FD-1.

**Parallelism:** parallel with FD-2 and FD-3.

### FD-5 — Structured privacy-safe diagnostic events

**Goal:** make failures diagnosable on-device and in test evidence without logging content.

Tasks:

- define structured event schema;
- add operation correlation IDs for import/analysis/export;
- record stable failure code, stage, timing and safe metadata;
- attach app/build/source identity where available;
- explicitly redact/omit document content and filename-sensitive fields;
- define bounded retention or ephemeral-only behavior;
- define how E2E/physical-device evidence captures failure codes;
- add tests that serialized diagnostics do not include representative sensitive payloads.

**Acceptance criteria:**

- a physical-device failure can be correlated to operation/build and canonical failure code;
- diagnostics contain enough metadata to identify the failing stage;
- sensitive content is absent by construction/test;
- retention/cleanup is bounded.

**Dependencies:** FD-1. Can begin before FD-2/3/4 finish.

**Parallelism:** parallel with FD-2, FD-3 and FD-4.

### FD-6 — Progressive diagnostic UI

**Goal:** expose useful detail without turning the normal product flow into a developer console.

Tasks:

- update `ProductErrorModel` or successor to carry user explanation, recovery action and technical-detail model;
- add an expandable/detail affordance for stable code/stage/operation ID where appropriate;
- keep raw stack traces and internal payloads out of the UI;
- ensure connection/import/analysis/export errors share consistent semantics and visual hierarchy;
- ensure accessibility semantics announce the error and recovery action;
- preserve adaptive layout and text scaling.

**Acceptance criteria:**

- primary error copy is understandable without technical knowledge;
- technical detail is available but secondary;
- every error surface has a valid next action or explicit terminal explanation;
- UI tests cover representative error states.

**Dependencies:** FD-2/FD-3/FD-4 contracts sufficiently stable; can be developed incrementally per domain.

### FD-7 — Failure-contract tests and architecture guardrails

**Goal:** make information loss and generic fallback regressions machine-detectable.

Required coverage:

- one deterministic mapping test per canonical failure cause;
- exhaustive mapping tests for enums/sealed hierarchies;
- explicit `IMAGE_ONLY_PDF -> RG-PDF-008` test;
- encrypted/malformed/limit/empty/parser-failed distinct projection tests;
- Harness connection and analysis cause mapping tests;
- retry-target/recovery-action tests;
- cancellation tests;
- export failure/cleanup tests;
- privacy tests for diagnostic serialization;
- unknown/unexpected fallback test;
- lifecycle tests proving failure paths release descriptors, parser service binding, analysis sessions and temporary export state.

Add architecture/static checks where practical to prevent product-critical `catch (Throwable)` / generic cause collapse from bypassing the canonical mapper.

**Dependencies:** incremental with FD-2 through FD-6.

**Parallelism:** tests should be implemented alongside each owning task, not deferred to the end.

### FD-8 — Physical-device/E2E failure evidence

**Goal:** validate the complete failure/recovery journey against real APK/device boundaries.

Extend the existing physical two-APK evidence runbook with representative cases:

- Host absent -> explicit Host failure -> recovery after Host becomes available;
- image-only PDF -> `RG-PDF-008` with correct recovery;
- encrypted/corrupt PDF fixture where practical;
- user cancellation during analysis;
- Host death/disconnect during analysis -> classified failure -> reconnect/retry;
- export destination/write failure -> cleanup -> retry with new destination;
- process recreation after an error does not resurrect sensitive document state;
- diagnostics captured with exact app/Harness build identities and without sensitive content.

**Acceptance criteria:** evidence records exact APK/build/device identity, expected stable code, visible recovery behavior and cleanup result.

**Dependencies:** FD-2 through FD-7 for the corresponding scenarios.

### FD-9 — Documentation, current-state and Definition of Done alignment

**Goal:** make the repository state truthful and durable.

Tasks:

- update `docs/current-state.md` while this workstream is active so repository-side completeness does not imply production-grade failure diagnostics prematurely;
- update architecture/security/observability docs with failure ownership and diagnostic privacy rules;
- update product experience documentation with the error/recovery contract;
- add failure classification/recovery/observability to repository Definition of Done;
- remove this active workstream after completion once durable knowledge has moved to canonical docs, consistent with `repo-template-sw` documentation lifecycle.

**Dependencies:** initial current-state correction can happen immediately; final durable-doc transfer follows implementation/evidence.

## Dependency graph

```text
FD-0 inventory
   |
   v
FD-1 canonical failure contract
   |
   +-------------------+-------------------+------------------+
   |                   |                   |                  |
   v                   v                   v                  v
FD-2 PDF/import     FD-3 analysis      FD-4 review/export  FD-5 diagnostics
   |                   |                   |                  |
   +-------------------+-------------------+------------------+
                       |
                       v
                 FD-6 diagnostic UI
                       |
                       v
                 FD-8 physical E2E

FD-7 tests/guardrails run continuously alongside FD-2..FD-6
FD-9 current-state starts immediately; final docs close after FD-8
```

## Recommended parallel execution

After FD-0 and FD-1 are complete, use four parallel lanes:

```text
Lane A: FD-2 PDF/import hardening + tests
Lane B: FD-3 Harness/analysis hardening + tests
Lane C: FD-4 review/export hardening + tests
Lane D: FD-5 structured diagnostics + privacy tests
```

FD-6 integrates the user-facing projections as each lane stabilizes rather than waiting for all implementation to finish. FD-8 remains the final real-device gate.

## Validation gates

Every implementation PR must run the repository canonical checks and the relevant focused tests. The exact commands remain owned by `.engineering/commands.json`.

Minimum evidence for this workstream:

```text
format/static checks
JVM/unit tests
Android Lint
APK build
failure mapping tests
lifecycle/cleanup tests
privacy-safe diagnostic tests
critical UI state tests
physical two-APK representative failure/recovery evidence
```

A green happy-path build alone is insufficient.

## Definition of Done

This workstream is complete only when all of the following are true:

- every known critical failure in import, connection, analysis, review and export has a canonical product-owned cause/code;
- known causes remain distinguishable through the final user/diagnostic projection;
- user-facing messages explain what happened and the next action when the cause is known;
- retry semantics are explicit and correct;
- technical detail is progressively disclosed;
- structured diagnostics can answer operation/stage/failure cause without recording sensitive document content;
- failure, cancellation, timeout/disconnect and cleanup paths have automated coverage where applicable;
- representative real-device failure/recovery paths have identity-bearing evidence;
- `docs/current-state.md` and durable architecture/product docs accurately describe the resulting behavior;
- no active completed implementation plan remains after durable documentation transfer.

## Explicit non-goals

This workstream does **not** implement OCR. `IMAGE_ONLY_PDF` must first become accurately diagnosable and actionable. OCR support, if desired, should be a separate capability/workstream with its own dependency, resource, privacy, performance and quality analysis.

This workstream also does not expose raw stack traces or create a general-purpose developer console inside RedactGuard. Technical diagnostics remain bounded and secondary to the user task.

## Immediate next actions

1. Mark this failure-diagnostics workstream active in the current-state ledger.
2. Complete FD-0 inventory across import/analysis/review/export.
3. Freeze the initial stable code registry and minimal Kotlin contract in FD-1.
4. Execute FD-2, FD-3, FD-4 and FD-5 in parallel with tests.
5. Integrate progressive UI diagnostics through FD-6.
6. Run the expanded physical failure/recovery evidence gate in FD-8.
7. Transfer durable rules into canonical docs and close/remove this workstream via FD-9.
