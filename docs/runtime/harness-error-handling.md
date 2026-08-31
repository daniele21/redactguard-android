# Harness error handling contract

RedactGuard treats Harness as an external local dependency. Failure of that dependency must never terminate the RedactGuard process.

## Product contract

| Runtime condition | RedactGuard state | User-facing behavior |
| --- | --- | --- |
| Host available and authorized | `CONNECTED` | Analysis enabled |
| Connection/negotiation in progress | `CONNECTING` | Explain that Harness is being verified |
| Package/signature/use-case authorization rejected | `PERMISSION_DENIED` | Explain that Harness rejected RedactGuard and recommend updating Harness |
| Protocol/feature mismatch | `INCOMPATIBLE` | Explain that the installed Harness version is incompatible and recommend updating it |
| Host package absent | `HOST_NOT_INSTALLED` | Explain that Harness must be installed |
| Binder connection lost or local connection failure | `DISCONNECTED` | Keep the app alive, explain the interruption, and allow a later retry |

Binder exception text is diagnostic data, not product copy. UI messages are deterministic and privacy-safe. Sensitive document state must not be serialized into connection diagnostics. Product diagnostics may retain only privacy-safe connection state and technical category; raw document content is never part of this error path.

During an analysis operation, typed Harness failures keep the existing RedactGuard product family while a bounded technical identity may be retained for diagnosis. Control Plane failures use `ControlPlane:<ENUM>`. Consumer prepare/session/generation failures use `Consumer:<ENUM>` together with the exact app-owned step that received them (`consumer.prepare`, `consumer.create-session` or `consumer.generate`). Free-form `ConsumerFailure.message`, Binder details and model/document payloads are never copied into `AnalysisRuntimeDiagnostic`.

This means a connected `RUNTIME_FAILURE`, `PREPARE_FAILED` or `SESSION_NOT_FOUND` remains a generation-family failure and therefore surfaces as `RG-AI-008 CHUNK_FAILED`; if transport is already lost it remains `DISCONNECTED`. The lower-level enum identity exists only inside progressively disclosed technical diagnostics so root-cause investigation does not require weakening product failure stability or privacy boundaries.

## Safety boundary

The published Harness Consumer SDK owns Binder/protocol exception normalization. RedactGuard keeps an additional product-level `connect()` boundary so synchronous `SecurityException` or runtime failures cannot escape Activity/ViewModel startup. The Activity retries connection from `onResume`, making recovery possible after the user installs or updates Harness without reinstalling RedactGuard.

Unexpected unchecked SDK/platform failures are normalized separately from typed Harness failures. They become the app-owned Local AI internal family with only a whitelisted exception type and safe boundary step; the exception message is discarded. Typed Harness failures must not be reclassified as unchecked exceptions or reduced to raw message strings.
