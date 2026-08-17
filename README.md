# RedactGuard Android

RedactGuard is a privacy-first Android application for detecting, reviewing and redacting personally identifiable information from PDF documents on-device.

The application is intentionally a **consumer** of the Android Local AI Harness rather than an owner of an LLM runtime. RedactGuard owns document handling, PII policy, structured result validation, human review and PDF export; the Harness host owns models, local inference, runtime scheduling and the public Android Consumer API.

## Repository status

This repository is being bootstrapped from the engineering baseline defined by `daniele21/repo-template-sw`, using the Android profile. The migration of the existing OMBRA implementation from `daniele21/android-local-llm-harness` is tracked in `docs/workstreams/ombra-to-redactguard-migration.md` on the development branch.

## Intended boundary

```text
RedactGuard APK
    |
    | Local AI Consumer SDK
    v
Android Binder IPC
    |
    v
Local AI Harness Host APK
    |
    v
Host-owned local model/runtime
```

RedactGuard must not package or directly depend on `llama.cpp`, GGUF artifacts, Harness runtime/model-store internals or backend-specific native structures.

## Engineering baseline

The repository follows the common engineering semantics from `repo-template-sw` with stack-native Android/Gradle tooling. The canonical project operations, architecture, current state and active workstreams are added as the bootstrap progresses.
