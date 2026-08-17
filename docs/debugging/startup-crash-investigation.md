# RedactGuard startup crash investigation

Status: ACTIVE
Owner: RedactGuard

## Problem

A Play/Internal-testing RedactGuard release has been observed to close immediately after launch. The repository previously validated only the debug variant, while the Play artifact is the minified release variant.

## Startup contract

Launching RedactGuard must be safe under all of these conditions:

- Harness installed and authorized;
- Harness absent;
- Harness present but permission denied / signer mismatch;
- Harness incompatible or temporarily disconnected.

None of these host states may terminate the RedactGuard process. They must project into the product connection state.

## Startup path under investigation

1. Android creates `MainActivity`.
2. `ViewModelProvider` creates `RedactGuardProductViewModel`.
3. The ViewModel constructs document infrastructure and `BinderAnalysisRuntimeComposition`.
4. The ViewModel initializes product UI state.
5. The ViewModel connects to Harness.
6. Compose renders the import screen.

The Harness Binder client already maps missing host and bind-time `SecurityException` to connection states, so a normal host absence/signature denial is not expected to crash the process.

## Confirmed validation gap

The Play build uses `release` with R8/minification enabled. CI historically compiled/tested/linted/assembled only debug. Therefore a green debug CI did not prove that the Play artifact could be shrunk/optimized successfully.

## Hardening actions

- Add an unsigned `assembleRelease` CI gate with R8 enabled. The artifact is validation-only and must never be distributed.
- Preserve fail-closed production signing: distributable release builds still require explicit upload signing configuration.
- Add startup-focused tests where practical rather than treating UI launch as implicitly covered by JVM domain tests.
- Do not disable R8 as a workaround. Any shrinker issue must be fixed through correct consumer/keep rules or code structure.

## Runtime evidence needed if static/release gates pass

Capture the first `FATAL EXCEPTION` from the Play-installed package with `adb logcat`. The exact exception/class/stack frame is required before attributing a runtime-only crash to signing, Binder, Compose, PDF infrastructure, or R8.
