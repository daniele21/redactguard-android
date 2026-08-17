# RedactGuard startup crash investigation

Status: ACTIVE — repository fixes implemented; physical Play runtime evidence still required
Owner: RedactGuard + Harness

## Problem

A Play/Internal-testing RedactGuard release has been observed to close immediately after launch. The repository historically validated only the debug variant, while the Play artifact is the minified release variant.

## Startup contract

Launching RedactGuard must be safe under all of these conditions:

- Harness installed and authorized;
- Harness absent;
- Harness present but permission denied / signer mismatch;
- Harness incompatible or temporarily disconnected.

None of these host states may terminate the RedactGuard process. They must project into the product connection state.

## Startup path reviewed

1. Android creates `MainActivity`.
2. `ViewModelProvider` creates `RedactGuardProductViewModel`.
3. The ViewModel constructs document infrastructure and `BinderAnalysisRuntimeComposition`.
4. The ViewModel initializes product UI state.
5. The ViewModel connects to Harness.
6. Compose renders the import screen.

No deterministic local constructor/null/`require` failure was found in the RedactGuard bootstrap path. The Harness Binder binding maps a missing host and bind-time `SecurityException` into connection states, so ordinary Host absence or signer/permission denial is not expected to terminate the process.

## Confirmed defect 1 — Consumer SDK touched a new AIDL API before negotiation

The published `consumer-android:0.1.0-alpha.1` constructed its AIDL consumer endpoint eagerly from `ILocalLlmService.consumerApi` inside the Android service-connected path. That transaction occurred before `protocolInfo` / feature negotiation could reject an older Host that did not expose `FEATURE_CONSUMER_API_V1`.

Therefore a newer consumer binding to an older Host could fail from the main-thread service callback before the normal transport-error boundary classified the Host as `INCOMPATIBLE`.

Harness fixes this in `consumer-android:0.1.0-alpha.2` by resolving the Consumer AIDL endpoint lazily, after compatibility negotiation. Regression tests prove that remote-service construction and `protocolInfo()` do not touch `getConsumerApi`, while the consumer endpoint is resolved at most once when actually requested.

RedactGuard is pinned to `0.1.0-alpha.2` for the next Play build.

## Confirmed defect 2 — release/R8 was not part of CI

The Play build uses `release` with R8/minification enabled. CI historically compiled/tested/linted/assembled only debug. Therefore a green debug CI did not prove that the Play artifact could be shrunk/optimized successfully.

Adding an explicit `assembleRelease` gate immediately exposed a real release-only failure:

- R8 missing class: `com.gemalto.jp2.JP2Decoder`;
- reference originates from PdfBox-Android `JPXFilter`;
- PdfBox-Android documents JP2/JPX decoding as an optional dependency and intentionally does not ship the Gemalto decoder by default.

The app therefore suppresses only this known optional R8 reference. It does not disable minification and does not add the legacy optional JP2 dependency merely to satisfy the shrinker.

## Release identity

The corrected Internal Testing build is assigned:

- `versionCode=2`;
- `versionName=0.1.1`;
- Harness Consumer SDK `0.1.0-alpha.2`.

Version identity is centralized in `app/version.properties` instead of being embedded as literals in the Android build script.

## Hardening actions

- Keep an unsigned `assembleRelease` CI gate with R8 enabled. The artifact is validation-only and must never be distributed.
- Preserve fail-closed production signing: distributable release builds still require explicit upload signing configuration.
- Keep the Binder compatibility regression in Harness so incompatible Hosts become typed connection state rather than consumer-process failure.
- Keep R8 enabled; fix release-only dependency/consumer-rule issues precisely.
- Capture startup evidence from the actual Play-installed package with `scripts/capture-startup-crash.sh`.

## Runtime evidence still required

Repository evidence proves the two defects above and validates their corrected build paths, but it does not by itself prove which exception terminated the already-installed Play build.

For the next Play-installed build, run:

```bash
bash scripts/capture-startup-crash.sh --device <SERIAL>
```

The capture records RedactGuard and Harness package/version/installer identity, launches RedactGuard from a clean logcat buffer, checks whether the process survives, and records only Android startup/runtime diagnostics. Do not open or import a document during this capture.

The migration incident is closed only when the corrected Play build stays alive or, if it still terminates, the first `FATAL EXCEPTION` is captured and resolved against the exact installed build identities.
