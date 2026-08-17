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
- committed Gradle 9.5.0 wrapper and pinned JDK/SDK/build-tool contract;
- bounded Gradle/Android build memory configuration and CI validation;
- product-owned PII definition and strict-JSON domain slice;
- deterministic document/segment domain slice plus isolated PDF extraction work;
- product-owned Compose presentation slice without Harness design-system dependency;
- frozen synthetic quality corpus/policy migration preserving the existing v2 identity;
- Harness RedactGuard authorization has passed repository validation;
- Harness Consumer SDK publication has already been proven from a separate external Gradle root and is completing ABI/publication hardening.

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

Do not remove the in-repo OMBRA implementation from Harness until the independently built two-APK proof is green.
