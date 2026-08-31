# Background/process lifecycle hardening

Status: active
Document type: workstream
Owner: RedactGuard analysis orchestration
Canonical scope: workstream.background-process-lifecycle
Started: 2026-08-31

## Outcome

Keep RedactGuard's product workflow coherent across UI recreation, app switching and transient Harness/Binder loss without moving model/runtime ownership into RedactGuard or persisting sensitive document/model content.

Harness remains authoritative for model/runtime/residency/Binder Host and logical inference-job behavior. RedactGuard owns document workflow, product progress, privacy policy, review/export and recovery UX.

## Invariants

- Product workflow state and transport connection state are separate state machines.
- Activity/ViewModel lifecycle must not be interpreted as user cancellation.
- Binder loss is `RECONNECTING`/reconciliation unless an authoritative Harness snapshot says the logical job failed.
- User cancellation is explicit and idempotent; cancellation intent can be represented with privacy-safe metadata.
- Document text, extracted segments, findings, reveal state, prompts and generated model output remain process-local by default.
- Durable metadata must not be sufficient to reconstruct sensitive document/model content.
- Automatic recovery is promised only when required process-local input still exists; otherwise recovery explicitly requires reopening/reimporting the source.

## Parallel lanes

### Lane A — pure product state

| ID | State | Task |
| --- | --- | --- |
| RGB-10 | IN_PROGRESS | Add stable redaction-job identity, product lifecycle state, transport state and recovery capability. |
| RGB-11 | IN_PROGRESS | Add monotonic revision/attempt rules and stale-update rejection tests. |
| RGB-12 | TODO | Add privacy-safe cancellation-intent and Harness-job correlation metadata. |

### Lane B — coordinator lifetime

| ID | State | Task |
| --- | --- | --- |
| RGB-20 | TODO | Move active analysis ownership out of `viewModelScope` into a process-scoped coordinator. |
| RGB-21 | TODO | Make ViewModel an observer/intent surface instead of coroutine owner. |
| RGB-22 | TODO | Keep sensitive workflow payload process-local inside the coordinator and clear it deterministically. |
| RGB-23 | TODO | Add explicit process-death recovery capability when the source/payload is unavailable. |

### Lane C — Harness transport reconciliation

| ID | State | Task |
| --- | --- | --- |
| RGB-30 | TODO | Separate connection/reconnect state from analysis product state. |
| RGB-31 | TODO | Reconcile authoritative Harness logical-job snapshot after reconnect. |
| RGB-32 | TODO | Use stable idempotency key per redaction job/operation/chunk once Consumer SDK exposes it. |
| RGB-33 | TODO | Persist/retry explicit cancel intent across reconnect without persisting sensitive payload. |

### Lane D — UX and evidence

| ID | State | Task |
| --- | --- | --- |
| RGB-40 | TODO | Project reconnect/recovery states as product guidance, not generic failure. |
| RGB-41 | TODO | E2E app-switch/activity-recreation/navigation journey. |
| RGB-42 | TODO | E2E Consumer process-death/reopen/reconcile journey. |
| RGB-43 | TODO | E2E Binder-loss/cancel-while-disconnected journey. |
| RGB-44 | TODO | Require screenshots + privacy-safe state artifact for each UI lifecycle journey. |
| RGB-45 | TODO | STRONG automated preflight, then representative same-signer two-APK real-GGUF validation. |

## Current coupling to remove

`RedactGuardProductViewModel` currently constructs the Binder runtime, owns `SequentialDocumentAnalyzer`, starts import/export work in `viewModelScope`, and explicitly cancels the active analysis plus closes the runtime from `onCleared()`. That is acceptable for a process-local prototype but is not the target lifecycle for user-visible background work.

The refactor must not merely delete cleanup calls: work launched in `viewModelScope` would still be cancelled on ViewModel clearance. Ownership must move to an explicit longer-lived process/work coordinator with deterministic close semantics.

## Recovery truth table

| Event | Required behavior |
| --- | --- |
| Compose recomposition | no semantic effect |
| Activity recreation | reconnect UI observer to same process job |
| navigate inside RedactGuard | no semantic effect |
| app background, process alive | analysis continues if Harness/Android execution policy allows |
| Binder transient disconnect | transport reconnecting; job not failed solely because transport vanished |
| RedactGuard process death, Harness alive | reconnect and recover same Harness logical job when protocol supports it |
| RedactGuard + sensitive source state lost | metadata may say recovery is required, but source must be reopened/reimported |
| Harness process death | consume authoritative interrupted/recovery state; never fabricate completion |
| explicit Cancel | persist safe intent, deliver/retry cancel, finish only after authoritative cancellation/terminal reconciliation |

## Validation

Pure state/reducer work uses deterministic JVM tests while iterating. Any Consumer SDK/Binder, persistence, WorkManager/service, manifest, packaging or cross-boundary change requires STRONG validation. Emulator evidence remains simulated/emulated and does not replace final physical same-signer Harness + RedactGuard + real-GGUF evidence.