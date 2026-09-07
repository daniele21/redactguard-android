# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-07

## Integrated state

RedactGuard is a privacy-first Android document-protection product consuming Harnex over the public Consumer Android SDK/Binder boundary. Harnex owns model/runtime policy; RedactGuard owns document, PII, review, redaction and export behavior.

`dev` contains the signer-aware Harnex integration and consumes `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.11`. RedactGuard binds explicitly without a custom Harnex bind permission, supports Consumer-first/Host-later installation, and remains fail-closed until Harnex authorizes the exact Binder-derived package/signer identity. Settings exposes Connect / Disconnect / Retry; `disconnect()` creates a reversible fresh-connection lifecycle without changing Harnex authority.

## Evidence

Automated evidence covers:

- RELEASE/FULL repository validation on previously qualified exact candidates;
- distinct-signer Consumer-first install, Host-later recovery, `PENDING`, exact authorization, Disconnect/Reconnect and replacement-signer `SIGNATURE_CHANGED` denial;
- Two-APK product/lifecycle/fault journeys;
- identity-bearing debug/release-ci packaging and Play Internal publication.

The focused physical Play Internal run confirms the current pre-release pair on device: RedactGuard-first, Harnex-later without reinstall, truthful Host absence, source-observed `PENDING`, explicit authorization, Connect / Disconnect / Reconnect and representative production Consumer SDK/Binder/local-analysis execution.

The current Play App Signing SHA-256 digest is the same for Harnex and RedactGuard. Under the amended pre-release evidence contract this same-signer first-party topology is acceptable for the current `dev -> main` promotion because it is recorded truthfully and signing identity is not used as a trust shortcut. Deterministic distinct-signer authorization evidence remains mandatory. This physical run is **not** distinct-signer Play qualification.

The post-publication delta is documentation/governance/verification-policy only; RedactGuard app source and Android build configuration are unchanged, so the physical journey remains applicable to the current product tree.

## Release state

`main` remains the stable/release line. For this pre-release promotion, signer topology is not a blocker once the resulting exact HEAD/base passes required RELEASE/FULL automated validation.

Before the first public release that depends on independently signed application distribution, or before claiming physical distinct-signer Play readiness, rerun the focused Play journey with distinct observed Harnex and RedactGuard signing identities. The deterministic distinct-signer lane remains required regardless of first-party signing topology.

LAS-07 ARM64/JNI/GGUF/resource, memory/thermal/OEM and other claim-specific physical evidence remains separate and is not implied by this promotion. Canonical physical runbook: `docs/evidence/physical-two-apk.md`.

## Immediate next block

1. run RELEASE/FULL on the exact current `dev` HEAD against live `main`;
2. promote `dev` when required automated gates are green;
3. retain physical distinct-signer Play qualification as a blocking obligation for the first applicable public release;
4. continue LAS-07 and other physical/runtime evidence independently.

Product strategy lives in `docs/product.md`. Do not move Harnex model/runtime administration into RedactGuard, persist sensitive task content for recovery, add cloud fallback, or fabricate success/recovery states.
