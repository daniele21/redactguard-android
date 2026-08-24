# Product experience evidence

Status: required; native test package wired, device execution pending
Owner: RedactGuard UI / product experience
Last reviewed: 2026-08-24

This evidence gate is intentionally separate from `physical-two-apk.md`. It validates RedactGuard interaction, accessibility and adaptive behavior; it does not prove the Harness + RedactGuard integration workflow.

## Automated evidence owned by the repository

CI must build the native UI-test APK with:

```bash
./gradlew --no-daemon :app:assembleDebugAndroidTest
```

The instrumentation suite covers:

- import primary/secondary task actions and accessible local-AI status;
- definition-selection gating before analysis;
- hidden-by-default review values and blocked export while decisions remain pending;
- progressive disclosure of technical failure details.

A successful assembly proves the native tests compile and package. It does **not** prove that they executed on Android hardware or an emulator.

## Native instrumentation execution

Against one explicit Android target, record the exact RedactGuard source revision, APK identity, test APK identity, device/emulator identity and Android version, then run:

```bash
ANDROID_SERIAL=<SERIAL> ./gradlew --no-daemon :app:connectedDebugAndroidTest
```

The execution evidence is green only when the instrumentation task succeeds for the recorded target. Retain only bounded, privacy-safe reports; the tests use synthetic UI models and must not include document text or finding values from real users.

## Accessibility and adaptive manual evidence

On a physical Android device, verify the same build with:

1. TalkBack enabled: the current product pane, local-AI status, error state and expandable diagnostics are announced with meaningful task language.
2. Increased system font size/display scaling: primary actions, review decisions and error recovery remain reachable without clipped critical text.
3. Compact portrait: the primary journey remains single-column and the primary action is obvious.
4. Compact landscape or medium-width window: content remains bounded and usable without horizontal clipping.
5. Expanded/tablet-class width: content stays centered and bounded rather than stretching a phone layout indefinitely.
6. Review privacy: sensitive values are hidden on entry and reveal requires an explicit user action.
7. Error recovery: technical diagnostics remain collapsed by default and user recovery stays visible without opening them.

Record pass/fail per scenario with device model, Android version, window/orientation and exact source/build identity. Screenshots are optional and must contain only synthetic data.

## Completion boundary

Do not claim RTA-9 complete from CI assembly alone. RTA-9 repository implementation is complete when the native tests package and repository validation are green; real-environment product-experience evidence remains pending until instrumentation plus representative accessibility/adaptive checks are recorded.
