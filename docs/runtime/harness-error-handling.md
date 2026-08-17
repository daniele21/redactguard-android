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

## Safety boundary

The published Harness Consumer SDK owns Binder/protocol exception normalization. RedactGuard keeps an additional product-level `connect()` boundary so synchronous `SecurityException` or runtime failures cannot escape Activity/ViewModel startup. The Activity retries connection from `onResume`, making recovery possible after the user installs or updates Harness without reinstalling RedactGuard.
