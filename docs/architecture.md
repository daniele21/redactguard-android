# Architecture

Status: active
Last reviewed: 2026-08-17

## Purpose

RedactGuard is an Android document-redaction product. It is a consumer of the Android Local AI Harness, not a local-model runtime owner.

## High-level flow

```text
PDF URI
  -> RedactGuard document boundary
  -> bounded extraction + segmentation
  -> PII definition selection
  -> analysis request composition
  -> Harness Consumer Android SDK
  -> Binder IPC
  -> Harness Host / document-pii-detection
  -> host-owned Qwen/runtime
  -> structured result
  -> RedactGuard strict validation
  -> Review
  -> deterministic redaction
  -> new PDF export
```

## Ownership

### RedactGuard

- Storage Access Framework document access;
- parser/exporter lifecycle and cleanup;
- document normalization and segment/source identity;
- PII categories and custom definitions;
- request/schema construction;
- client-side result validation;
- human review and conflict decisions;
- deterministic redaction/export;
- product UI/accessibility;
- synthetic PII quality corpus and product-quality evaluation.

### Harness

- public Consumer API and Android SDK;
- Binder protocol/host service;
- caller authorization and compatibility;
- use-case/model/preset binding;
- model storage/verification/loading;
- runtime scheduling, inference, cancellation and memory/resource ownership;
- llama.cpp/native backend;
- host diagnostics and shared-runtime evidence.

## Dependency rule

Product code may depend on RedactGuard domain/application ports and on the published Harness Consumer Android SDK at the infrastructure boundary.

Product code must not depend on Harness source modules, runtime/model internals, generated Binder implementation details, native pointers or GGUF filesystem identity.

The RedactGuard build must remain possible from this repository plus declared artifact repositories; a local Harness source checkout is not a build prerequisite.

## Security boundary

The runtime boundary is Android Binder with signature/package/application/use-case authorization owned by Harness. RedactGuard does not choose model identity, private host paths or arbitrary runtime tuning.

Document text cannot select host package, use case, model, preset, schema destination or export destination.

Sensitive document/PII state remains process-local by default and is excluded from normal logs/telemetry. No implicit cloud fallback is permitted.

## Resource lifecycle

RedactGuard owns bounded document/parser/export resources and consumer sessions created for its operations. Success, failure and cancellation must deterministically close descriptors, parser/export state and consumer sessions owned by the operation.

Model residency, decode queues, KV cache and native runtime resources are explicitly outside RedactGuard ownership and remain in Harness.

## Validation layers

```text
pure domain tests
 -> PDF fixture tests
 -> fake Consumer SDK/application-port tests
 -> Compose semantics/screenshot tests
 -> packaged SDK contract tests
 -> cross-repository APK integration
 -> physical two-APK document E2E
 -> quality/security/release evidence
```

The canonical migration/dependency plan is `docs/workstreams/ombra-to-redactguard-migration.md`.
