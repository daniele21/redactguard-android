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
- product-owned PII definition and strict-JSON domain slice;
- deterministic document/segment domain slice;
- product-owned Compose presentation slice without Harness design-system dependency;
- frozen synthetic quality corpus/policy migration preserving the existing v2 identity;
- Harness-side external Consumer SDK publication and RedactGuard authorization are progressing in parallel in the Harness repository.

Current bootstrap validation uses an exact Gradle 9.5.0 installation in CI while the binary `gradle-wrapper.jar` remains an explicit RG-0 completion item. The wrapper distribution URL and SHA-256 are already pinned to the Harness toolchain.

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
