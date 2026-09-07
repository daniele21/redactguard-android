# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-07

## Integrated state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex owns model selection/configuration, GGUF/runtime, activation and residency.

The Local AI setup/readiness and lifecycle work is integrated together with text/PDF ingestion, PII selection, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive product UI and process-local sensitive state. OCR/VLM, cloud fallback, persisted History and fabricated progress/metrics remain out of scope.

The current integrated `dev` baseline contains the signer-independent Harnex connection work from PR #202 and consumes the immutable public Consumer SDK `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`. Repository governance is aligned to `repo-template-sw` `0.11.0` with proportional product-development routing plus the local Android/product-UI customizations recorded in `.engineering/baseline.json`.

## Local AI release candidate

RedactGuard no longer depends on a same-signer install topology. It binds explicitly to the Harnex Service without requesting a custom Harnex bind permission, so RedactGuard may be installed before Harnex without requiring reinstall. Reachability is not authorization: Harnex derives authority from Binder UID -> exact installed package -> current signer -> persisted Harnex Control Plane authorization -> enabled use case.

Settings owns the user's reversible connection preference and exposes source-backed Connect / Disconnect / Retry behavior. `disconnect()` is provided by Consumer SDK alpha.11; reconnect starts a fresh bind/authorization epoch. Authorization remains Harnex-owned, and RedactGuard opens Harnex when the observed app identity requires approval.

## Automated, publication and release evidence

On the final PR #202 candidate and its tree-equivalent integrated `dev` commit:

- normal repository `Validate` passed selector-chosen FULL validation while resolving the public alpha.11 dependency;
- Repository health passed;
- `Harnex independent-signer E2E` passed with distinct Host and Consumer signers, Consumer-first install, Host-later recovery, fail-closed `PENDING`, exact identity authorization, Settings Disconnect/Reconnect and replacement signer denial as `SIGNATURE_CHANGED`;
- `Two-APK emulator E2E` passed the cross-process product journey, ViewModel/Home continuity and Binder cancellation/process-loss/critical-pressure matrix;
- integration remote preflight passed `integration/full` on the exact candidate/base;
- post-merge `Validate`, Repository health and identity-bearing debug/release-ci packaging passed on `dev`;
- the signed RedactGuard release AAB was published successfully to Google Play Internal Testing as the current internal candidate.

Harnex alpha.11 is published publicly and the current Harnex phone-test candidate is also published to Google Play Internal Testing. Successful CI and Play upload do not by themselves establish the actual Play App Signing identities observed on a physical phone.

## REAL_ENVIRONMENT release gate

The remaining focused release evidence for the signer/install-order change is a physical Play Internal retest using the actual Play-distributed Harnex and RedactGuard applications:

1. install/update RedactGuard first with Harnex absent;
2. confirm Local AI is unavailable/fail-closed rather than faked as connected;
3. install/update Harnex from Play without reinstalling RedactGuard;
4. confirm RedactGuard becomes source-observed `PENDING` and Harnex shows the exact installed package/signer identity;
5. authorize that observed identity in Harnex and confirm RedactGuard connects;
6. exercise Settings Disconnect -> Reconnect;
7. run a representative local-analysis journey through the production Binder/runtime path;
8. record the actual Play signing identities and source/version identities used.

This focused Play signer/install-order confirmation is separate from the broader LAS-07 ARM64/JNI/GGUF/resource evidence bundle. Emulator evidence must not be relabeled as either physical proof.

## LAS status

LAS-00..06, LAS-08A/B/C and LAS-09..14 remain complete as deterministic implementation/evidence outcomes. LAS-07 owns broader representative real-environment claims that require physical Android ARM64 execution through production llama.cpp/JNI with a real compatible GGUF, lifecycle/resource evidence, thermal/OEM behavior where claimed and representative-device accessibility/usability confirmation where required.

The canonical runbook is `docs/evidence/physical-two-apk.md`.

## Stable release state

`main` remains the stable/release line. The current signer-independent alpha.11 candidate is integrated on `dev` and has passed all required automated integration gates and Play Internal publication. Before the next `dev -> main` promotion, the repository RELEASE contract requires FULL promotion validation plus the applicable focused physical Play signer/install-order confirmation above.

## Immediate next block

1. run and record the focused Play Internal Harnex/RedactGuard independent-signer install-order authorization retest;
2. run RELEASE/FULL promotion validation against the live `main` base;
3. promote the reconciled `dev` candidate to `main` only when the required release evidence is complete;
4. keep broader LAS-07 ARM64/GGUF/memory/thermal/OEM claims separate unless their canonical evidence is captured.

Product strategy and decision boundaries are owned by `docs/product.md`; current implementation/release state remains here.

Do not move Harnex model/runtime administration into RedactGuard, persist sensitive document/prompt/finding/output content for recovery, add cloud fallback, or map generic product incompatibility to an assumed Harnex bug. Product behavior must use typed failure identity; normal UI must express user-task problems and real recovery actions rather than Binder/Harnex internals.
