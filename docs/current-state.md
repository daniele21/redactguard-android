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

On the PR #202 candidate and tree-equivalent integrated `dev` commit:

- normal repository `Validate` passed selector-chosen FULL validation while resolving public alpha.11;
- Repository health passed;
- `Harnex independent-signer E2E` passed with distinct signers, Consumer-first install, Host-later recovery, fail-closed `PENDING`, exact authorization, Disconnect/Reconnect and replacement-signer denial as `SIGNATURE_CHANGED`;
- `Two-APK emulator E2E` passed the product journey, ViewModel/Home continuity and Binder cancellation/process-loss/critical-pressure matrix;
- integration remote preflight passed `integration/full` on the exact candidate/base;
- post-merge `Validate`, Repository health and identity-bearing debug/release-ci packaging passed on `dev`;
- the signed release AAB was published to Google Play Internal Testing.

Harnex alpha.11 is public and its current phone-test candidate is also on Play Internal Testing. CI and successful upload do not establish the actual Play App Signing identities observed on a physical phone.

## REAL_ENVIRONMENT release gate

The remaining focused evidence is a physical Play Internal retest using the actual Play-distributed Harnex and RedactGuard apps:

1. install/update RedactGuard first with Harnex absent;
2. confirm Local AI is unavailable/fail-closed;
3. install/update Harnex from Play without reinstalling RedactGuard;
4. confirm RedactGuard becomes source-observed `PENDING` and Harnex shows the exact installed package/signer identity;
5. authorize that identity in Harnex and confirm RedactGuard connects;
6. exercise Settings Disconnect -> Reconnect;
7. run a representative local-analysis journey through production Binder/runtime;
8. record Play signing and source/version identities.

This focused signer/install-order confirmation is separate from broader LAS-07 ARM64/JNI/GGUF/resource evidence. Emulator evidence must not be relabeled as physical proof.

## LAS status

LAS-00..06, LAS-08A/B/C and LAS-09..14 remain complete as deterministic implementation/evidence outcomes. LAS-07 owns broader claims requiring physical Android ARM64 execution through production llama.cpp/JNI with a compatible GGUF, lifecycle/resource evidence, thermal/OEM behavior where claimed and representative-device accessibility/usability confirmation where required.

Canonical runbook: `docs/evidence/physical-two-apk.md`.

## Stable release state

`main` remains the stable/release line. The signer-independent alpha.11 candidate is integrated on `dev` with required automated integration gates and Play Internal publication complete. The next `dev -> main` promotion still requires RELEASE/FULL plus the focused physical Play signer/install-order confirmation above.

## Immediate next block

1. run and record the focused Play Internal independent-signer/install-order authorization retest;
2. run RELEASE/FULL promotion validation against live `main`;
3. promote reconciled `dev` only when required release evidence is complete;
4. keep broader LAS-07 ARM64/GGUF/memory/thermal/OEM claims separate unless captured canonically.

Product strategy and decision boundaries live in `docs/product.md`; current implementation/release state remains here.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic incompatibility to an assumed Harnex bug. Product behavior uses typed failure identity; normal UI expresses user-task problems and real recovery actions rather than Binder/Harnex internals.
