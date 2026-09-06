# Harnex error handling contract

Last reviewed: 2026-09-06

RedactGuard treats Harnex as an external local dependency. Failure of that dependency must never terminate the RedactGuard process.

## Product contract

| Runtime condition | RedactGuard state | User-facing behavior |
| --- | --- | --- |
| Host available and authorized | `CONNECTED` | Connection is available; analysis still requires fresh setup/preflight readiness |
| Connection/negotiation in progress | `CONNECTING` | Explain that Harnex is being verified |
| Exact package/signer/use-case authorization rejected | `PERMISSION_DENIED` | Explain that Harnex authorization is required and offer to open Harnex |
| Protocol/feature mismatch | `INCOMPATIBLE` | Explain that the installed Harnex version is incompatible and recommend updating it |
| Host package absent | `HOST_NOT_INSTALLED` | Explain that Harnex must be installed |
| Binder connection lost or local connection failure | `DISCONNECTED` | Keep the app alive, explain the interruption, and allow a later retry |
| User explicitly disconnected in Settings | `DISCONNECTED` | Preserve the choice and do not auto-reconnect until the user explicitly connects |

Binder exception text is diagnostic data, not product copy. UI messages are deterministic and privacy-safe. Sensitive document state must not be serialized into connection diagnostics. Product diagnostics may retain only privacy-safe connection state and technical category; raw document content is never part of this error path.

During an analysis operation, typed Harnex failures keep the existing RedactGuard product family while a bounded technical identity may be retained for diagnosis. Control Plane failures use `ControlPlane:<ENUM>`. Consumer prepare/session/generation failures use `Consumer:<ENUM>` together with the exact app-owned step that received them (`consumer.prepare`, `consumer.create-session` or `consumer.generate`). Free-form `ConsumerFailure.message`, Binder details and model/document payloads are never copied into `AnalysisRuntimeDiagnostic`.

This means a connected `RUNTIME_FAILURE`, `PREPARE_FAILED` or `SESSION_NOT_FOUND` remains a generation-family failure and therefore surfaces as `RG-AI-008 CHUNK_FAILED`; if transport is already lost it remains `DISCONNECTED`. The lower-level enum identity exists only inside progressively disclosed technical diagnostics so root-cause investigation does not require weakening product failure stability or privacy boundaries.

## Authorization and install-order boundary

RedactGuard and Harnex may be independently signed and installed in either order. RedactGuard binds only to the explicit configured Harnex component and does not request a custom Harnex bind permission. Harnex derives Binder caller UID, exact installed RedactGuard package and current signing certificate, then applies persisted Harnex Control Plane authorization/use-case policy.

A new observed RedactGuard signer is pending until explicitly authorized in Harnex. A replacement signer cannot inherit prior authorization. Both cases surface through the existing `PERMISSION_DENIED` transport outcome because that SDK state represents Host authorization denial, not necessarily an Android manifest permission failure.

The recovery action is therefore not “update Harnex” by default and never “reinstall RedactGuard to refresh a permission.” RedactGuard directs the user to Harnex when authorization is required, retries after Harnex is installed/updated when the Host is absent/incompatible, and keeps signing-certificate mechanics out of normal product copy.

## Safety boundary

The published Harnex Consumer SDK owns Binder/protocol exception normalization. RedactGuard keeps an additional product-level `connect()` boundary so synchronous `SecurityException` or runtime failures cannot escape Activity/ViewModel startup.

Lifecycle connection attempts call the process-local owner, which respects the persisted Settings connection preference. An explicit Settings disconnect disables automatic reconnect and releases the current reusable Consumer SDK connection; later explicit Connect performs a fresh bind/negotiation/authorization epoch. Disconnect is not an analysis cancellation and is blocked while a non-terminal analysis is active.

Unexpected unchecked SDK/platform failures are normalized separately from typed Harnex failures. They become the app-owned Local AI internal family with only a whitelisted exception type and safe boundary step; the exception message is discarded. Typed Harnex failures must not be reclassified as unchecked exceptions or reduced to raw message strings.
