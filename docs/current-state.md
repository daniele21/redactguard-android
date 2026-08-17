# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Read when: determining what is implemented, active or blocked in RedactGuard
Last reviewed: 2026-08-17

## Repository state

RedactGuard is being extracted from the previously integrated OMBRA application in `daniele21/android-local-llm-harness`.

Implemented on active migration branches:

- Android/Compose repository shell with final RedactGuard package identity;
- committed Gradle 9.5.0 wrapper and pinned JDK/SDK/build-tool contract; the repaired wrapper is byte-identical to the validated Harness wrapper blob `b1b8ef56b44f16b14dc800fa8103a6d89abb526f`;
- bounded Gradle/Android build memory configuration and CI validation;
- product-owned PII definition and strict-JSON domain slice;
- deterministic document/segment domain slice plus isolated PDF extraction work;
- product-owned Compose presentation slice without Harness design-system dependency;
- frozen synthetic quality corpus/policy migration preserving the existing v2 identity;
- Harness RedactGuard authorization is integrated in Harness `dev`;
- Harness external Consumer Android SDK publication, external-project consumption proof and public ABI gate are integrated in Harness `dev` at `5e910640f476a83f7e4d12234aa14bc63d78b4a9`.

## Critical path

```text
RG-0/RG-2/RG-3/RG-4/RG-5      HSDK-1 + HHOST-1
             \                    /
              +------> RG-6 <----+
                        |
                        v
                physical cross-repo E2E
                        |
                        v
                  Harness cutover
```

RG-6 now depends on converging the RedactGuard migration slices and resolving a published Consumer SDK version from an external artifact repository. Runtime behavioral proof remains part of RG-6/E2E rather than the compile-only SDK publication gate.

Do not remove the in-repo OMBRA implementation from Harness until the independently built two-APK proof is green.
