# RedactGuard Android Visual Reference Convergence

Status: active
Document type: workstream
Owner: redactguard-android
Reference approved: 2026-08-26
Goal: converge the settled RedactGuard Android task model on the approved five-step visual reference while preserving Android-native behavior, privacy boundaries and Harness runtime ownership.

## Canonical inputs

Semantic decisions remain owned by `design/ux-contract.json`. Brand/tokens are owned by `design/brand-kit.json`. Visual composition is owned by the approved Android reference and the durable adaptation rules in `design/reference/README.md`. Production Compose remains the executable implementation, not a second design specification.

## Non-goals

- no OCR/VLM or image-only PDF support;
- no persisted history or bottom navigation;
- no generic Options destination without a product job;
- no cloud fallback;
- no Harness/model/runtime administration inside RedactGuard;
- no exact PDF-coordinate preview;
- no fabricated analysis percentage or placeholder metrics;
- no decorative motion that changes or delays privacy/recovery actions.

## Completion rule

This workstream is not DONE merely because semantic hierarchy, tokens or Compose tests are green. Completion requires all of the following:

1. approved reference decisions/exclusions are durable in-repo;
2. shared app shell/components own repeated visual semantics;
3. Document, Protection, Analysis, Review and Outcome/Recovery converge on the approved hierarchy;
4. compact and wider Android layouts preserve content/action priority;
5. deterministic component/instrumentation tests cover critical semantics and state hierarchy;
6. representative screenshots exist for the stable high-risk surfaces with source/build identity;
7. representative physical-device TalkBack, large-text and adaptive evidence is recorded on a named device.

## Parallel execution graph

```text
VUI-1 approved visual baseline
        |
        v
VUI-2 shared visual system / shell
   |          |            |
   v          v            v
VUI-3      VUI-4        VUI-5
Document   Protection   Analysis/Review/Outcome
   \          |            /
    \         |           /
             VUI-6 adaptive convergence
                    |
                    v
             VUI-7 visual/device evidence
```

VUI-3, VUI-4 and VUI-5 are intentionally separate source owners after VUI-2 so work can proceed concurrently without multiple slices rewriting one screen monolith.

## Slices

| Slice | Status | Depends on | Owns / writes | Validation |
| --- | --- | --- | --- | --- |
| VUI-1 visual reference baseline | DONE | none | `design/reference/README.md`, `design/brand-kit.json` | reference/adaptation review |
| VUI-2 shared visual system & app shell | ACTIVE | VUI-1 | `RedactGuardScreens.kt`, `RedactGuardVisualPrimitives.kt`, theme tokens, reference vectors | exact-head Kotlin + semantics |
| VUI-3 Document / Import experience | ACTIVE | VUI-2 | `DocumentProtectionScreens.kt` import surface | Compose/build + compact screenshot |
| VUI-4 Protection selection experience | ACTIVE | VUI-2 | `ProtectionSelectionScreen.kt` | Compose/build + selection semantics + screenshot |
| VUI-5 Analysis, Review & Outcome experience | ACTIVE | VUI-2 | `DocumentProtectionScreens.kt` analysis, `ReviewScreen.kt`, `ProductFlowScreens.kt` | Compose/build + review/outcome tests |
| VUI-6 Adaptive / tablet experience | ACTIVE | VUI-2,VUI-5 | review window composition and adaptive evidence | medium/expanded semantics + screenshot |
| VUI-7 visual, accessibility & device evidence | BLOCKED | VUI-3,VUI-4,VUI-5,VUI-6 | bounded evidence only | 7-shot baseline + TalkBack + large text + compact landscape |

## Implementation direction

### Document / Import

Match the approved document-first hero, large input action cards and subordinate local/privacy guidance. `Importa un PDF` is dominant; pasted text remains secondary. The RedactGuard shield may reinforce the task but must not carry required meaning.

### Protection

Use 2-column profile decision cards where compact width permits. Detailed definitions remain individually controllable and map visually into the six approved PII families without changing domain semantics. Selected state is never color-only. `Analizza in locale` remains the dominant exit action.

### Analysis

Use one focused local-processing surface. Show truthful phases and indeterminate progress unless deterministic work-unit progress becomes available. Cancellation remains secondary and immediately reachable.

### Review

Keep deterministic occurrence progress, PII category, masked source context, hidden value and redact/keep decision easy to scan. Category color is supportive. Compact is single-focus; wider layouts keep context and decision side by side. Do not invent the approved mockup's richer left summary rail until truthful filename/page/category-count projections exist.

### Outcome / Recovery

Success uses the approved green protected-document language without fabricated counts or file metadata. Error leads with cause/recovery; diagnostics remain collapsed and privacy-safe.

## Evidence policy

Software CI can prove formatting, compilation, semantics, tests, lint and packaging. It cannot prove visual convergence. VUI-2 through VUI-6 remain ACTIVE until the current implementation head passes repository validation; VUI-7 remains BLOCKED until actual screenshots/device evidence are recorded. The PR must remain draft while the stronger visual-complete claim is not established.
