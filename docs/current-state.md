# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-07

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

Local AI setup/readiness, text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive UI and process-local sensitive state are integrated. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

`dev` contains the signer-aware Harnex connection work and consumes immutable public Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`. Governance is aligned to `repo-template-sw` `0.11.0` with proportional product-development routing and local Android/product-UI customizations in `.engineering/baseline.json`.

## Local AI release candidate

RedactGuard binds explicitly to the Harnex Service without a custom Harnex bind permission, so it may be installed before Harnex without reinstall. Reachability is not authorization: Harnex derives authority from Binder UID -> exact installed package -> current signer -> persisted Control Plane authorization -> enabled use case.

Settings owns the reversible connection preference and exposes source-backed Connect / Disconnect / Retry. Consumer SDK alpha.11 provides `disconnect()`; reconnect starts a fresh bind/authorization epoch. Authorization remains Harnex-owned, and RedactGuard opens Harnex when the observed app identity requires approval.

## Automated, publication and release evidence

Automated release evidence covers the current integration line:

- repository FULL validation and Repository health are green on the previously qualified exact candidates;
- `Harnex independent-signer E2E` proves distinct-signer Consumer-first install, Host-later recovery, fail-closed `PENDING`, exact authorization, Disconnect/Reconnect and replacement-signer denial as `SIGNATURE_CHANGED`;
- `Two-APK emulator E2E` proves the product journey, ViewModel/Home continuity and Binder cancellation/process-loss/critical-pressure matrix;
- identity-bearing debug/release-ci packaging passed;
- the signed release AAB was published to Google Play Internal Testing.

Harnex alpha.11 is public and its current phone-test candidate is also on Play Internal Testing.

## REAL_ENVIRONMENT release evidence

The focused physical Play Internal run confirms the real current distribution journey:

1. RedactGuard first with Harnex absent and truthful fail-closed Local AI unavailability;
2. Harnex installed/updated later without reinstalling RedactGuard;
3. source-observed `PENDING` and the exact installed RedactGuard identity in Harnex;
4. explicit authorization and successful RedactGuard recovery;
5. Settings Disconnect -> Reconnect;
6. representative production Consumer SDK/Binder/runtime local analysis.

The Play signing metadata collected for this run reports the **same current Play App Signing SHA-256 digest for Harnex and RedactGuard**. Therefore this physical evidence proves the actual same-signer Play topology but does not prove the repository-declared distinct-signer REAL_ENVIRONMENT topology. The automated distinct-signer lanes remain authoritative for the cross-signer authorization semantics they directly exercise, but they are not physical Play signer evidence.

This focused signer-fidelity gap is separate from broader LAS-07 ARM64/JNI/GGUF/resource evidence. Emulator evidence must not be relabeled as physical proof.

The commits added after the published/runtime-qualified RedactGuard candidate are limited to documentation, repository governance, verification scripts and workflow-policy surfaces; no RedactGuard app source or Android build configuration changed. The functional physical journey therefore remains applicable to the current product/runtime tree, but the signer-fidelity release gate remains unresolved under the current contract.

## LAS status

LAS-00..06, LAS-08A/B/C and LAS-09..14 remain complete as deterministic implementation/evidence outcomes. LAS-07 owns broader claims requiring physical Android ARM64 execution through production llama.cpp/JNI with a compatible GGUF, lifecycle/resource evidence, thermal/OEM behavior where claimed and representative-device accessibility/usability confirmation where required.

Canonical runbook: `docs/evidence/physical-two-apk.md`.

## Stable release state

`main` remains the stable/release line. The alpha.11 candidate is integrated on `dev`, automated validation and Play Internal publication are established, and the real install-order/authorization/runtime journey works on the current Play pair. Stable promotion is nevertheless blocked by the mismatch between the current same-signer Play distribution and the repository-declared distinct-signer physical release requirement.

Close this in one of two legitimate ways:

- configure/obtain distinct Play App Signing identities and rerun the focused physical journey; or
- if same-signer first-party distribution is intentionally the desired product topology, deliberately reshape the product/security/evidence contract and affected ADR/E2E/runbook claims before promotion, while retaining deterministic proof that distinct-signer Consumers remain supported and fail closed correctly.

## Immediate next block

1. resolve the Play signer-topology decision without silently weakening the current blocking release contract;
2. once the applicable real-environment evidence/contract is truthful and complete, rerun RELEASE/FULL on the resulting exact `dev` HEAD against live `main`;
3. promote reconciled `dev` only when required release evidence is complete;
4. keep broader LAS-07 ARM64/GGUF/memory/thermal/OEM claims separate unless captured canonically.

Product strategy and decision boundaries live in `docs/product.md`; current implementation/release state remains here.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic incompatibility to an assumed Harnex bug. Product behavior uses typed failure identity; normal UI expresses user-task problems and real recovery actions rather than Binder/Harnex internals.
