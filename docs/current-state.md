# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-07

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

Local AI setup/readiness, text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive UI and process-local sensitive state are integrated. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

`dev` contains the signer-independent Harnex connection work and consumes immutable public Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`. Governance is aligned to `repo-template-sw` `0.11.0` with proportional product-development routing and local Android/product-UI customizations in `.engineering/baseline.json`.

## Local AI release candidate

RedactGuard binds explicitly to the Harnex Service without a custom Harnex bind permission, so it may be installed before Harnex without reinstall. Reachability is not authorization: Harnex derives authority from Binder UID -> exact installed package -> current signer -> persisted Control Plane authorization -> enabled use case.

Settings owns the reversible connection preference and exposes source-backed Connect / Disconnect / Retry. Consumer SDK alpha.11 provides `disconnect()`; reconnect starts a fresh bind/authorization epoch. Authorization remains Harnex-owned, and RedactGuard opens Harnex when the observed app identity requires approval.

## Automated, publication and release evidence

Automated release evidence covers the current integration line:

- normal repository `Validate` passed selector-chosen FULL validation while resolving public alpha.11;
- Repository health passed;
- `Harnex independent-signer E2E` passed with distinct signers, Consumer-first install, Host-later recovery, fail-closed `PENDING`, exact authorization, Disconnect/Reconnect and replacement-signer denial as `SIGNATURE_CHANGED`;
- `Two-APK emulator E2E` passed the product journey, ViewModel/Home continuity and Binder cancellation/process-loss/critical-pressure matrix;
- integration remote preflight passed `integration/full` on the applicable exact candidate/base;
- post-merge identity-bearing debug/release-ci packaging passed on `dev`;
- the signed release AAB was published to Google Play Internal Testing.

Harnex alpha.11 is public and its current phone-test candidate is also on Play Internal Testing.

## REAL_ENVIRONMENT release evidence

The focused physical Play Internal signer/install-order gate is now operator-confirmed on the current Play-distributed Harnex and RedactGuard pair. The confirmed journey covers:

1. RedactGuard first with Harnex absent and truthful fail-closed Local AI unavailability;
2. Harnex installed/updated later without reinstalling RedactGuard;
3. source-observed `PENDING` and the exact installed RedactGuard identity in Harnex;
4. explicit authorization of that observed identity and successful RedactGuard recovery;
5. Settings Disconnect -> Reconnect;
6. representative production Consumer SDK/Binder/runtime local analysis;
7. current Play-installed Harnex and RedactGuard using independent signing identities.

Exact certificate digest values remain release-evidence metadata and are not duplicated in this operational ledger. This focused signer/install-order confirmation is separate from broader LAS-07 ARM64/JNI/GGUF/resource evidence; passing it does not relabel emulator evidence or close unrelated physical-runtime claims.

The commits added after the previously published/runtime-qualified RedactGuard candidate up to the current release line are limited to documentation, repository governance, verification scripts and workflow-policy surfaces; no RedactGuard app source or Android build configuration changed. The physical Play result therefore remains applicable to the current product/runtime tree while deterministic RELEASE/FULL validation is rerun on the new exact repository HEAD.

## LAS status

LAS-00..06, LAS-08A/B/C and LAS-09..14 remain complete as deterministic implementation/evidence outcomes. LAS-07 owns broader claims requiring physical Android ARM64 execution through production llama.cpp/JNI with a compatible GGUF, lifecycle/resource evidence, thermal/OEM behavior where claimed and representative-device accessibility/usability confirmation where required.

Canonical runbook: `docs/evidence/physical-two-apk.md`.

## Stable release state

`main` remains the stable/release line. The signer-independent alpha.11 candidate is integrated on `dev`, automated release validation has been green, Play Internal publication is complete and the focused blocking physical Play signer/install-order gate is now confirmed. The evidence-ledger update itself moves the exact `dev` HEAD, so RELEASE/FULL must be green again against live `main` before promotion.

## Immediate next block

1. run RELEASE/FULL promotion validation against live `main` on the new exact `dev` HEAD;
2. promote reconciled `dev` to `main` if the exact-head/base release evidence remains green;
3. keep broader LAS-07 ARM64/GGUF/memory/thermal/OEM claims separate unless captured canonically.

Product strategy and decision boundaries live in `docs/product.md`; current implementation/release state remains here.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic incompatibility to an assumed Harnex bug. Product behavior uses typed failure identity; normal UI expresses user-task problems and real recovery actions rather than Binder/Harnex internals.
