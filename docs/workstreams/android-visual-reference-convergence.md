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
6. exact-head emulator E2E covers the required pasted-text, text-PDF and Local-AI recovery journeys;
7. representative screenshots exist for the stable high-risk surfaces with source/build identity;
8. representative physical-device TalkBack, large-text and adaptive evidence is recorded on a named device.

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
       emulator visual-evidence gate
                    |
                    v
       emulator product-journey E2E
                    |
                    v
          VUI-7 physical-device gate
```

VUI-3, VUI-4 and VUI-5 are intentionally separate source owners after VUI-2 so work can proceed concurrently without multiple slices rewriting one screen monolith.

## Slices

| Slice | Status | Depends on | Owns / writes | Validation |
| --- | --- | --- | --- | --- |
| VUI-1 visual reference baseline | DONE | none | `design/reference/README.md`, `design/brand-kit.json` | reference/adaptation review |
| VUI-2 shared visual system & app shell | DONE | VUI-1 | `RedactGuardScreens.kt`, `RedactGuardVisualPrimitives.kt`, theme tokens, reference vectors | exact-head Kotlin + semantics |
| VUI-3 Document / Import experience | DONE | VUI-2 | `DocumentProtectionScreens.kt` import surface | Compose/build + compact screenshot + journey E2E |
| VUI-4 Protection selection experience | DONE | VUI-2 | `ProtectionSelectionScreen.kt` | Compose/build + selection semantics + screenshot |
| VUI-5 Analysis, Review & Outcome experience | DONE | VUI-2 | `DocumentProtectionScreens.kt` analysis, `ReviewScreen.kt`, `ProductFlowScreens.kt` | Compose/build + review/outcome tests + journey E2E |
| VUI-6 Adaptive / tablet experience | DONE | VUI-2,VUI-5 | review window composition and adaptive evidence | medium/expanded semantics + screenshot |
| VUI-7 physical accessibility/device evidence | ACTIVE | VUI-3,VUI-4,VUI-5,VUI-6 | bounded named-device evidence only | TalkBack + large text + compact landscape + real two-APK product flow |

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

## Emulator visual-evidence gate

`.github/workflows/visual-evidence.yml` renders seven stable reference surfaces on a controlled Android emulator and retains the screenshots for seven days as a source-revision-addressed GitHub Actions artifact. The compact run uses a 1080x2400 logical display at 420 dpi. The expanded run uses a 1600x2560 logical display at 320 dpi and forces the settled expanded review composition.

The instrumentation evidence writer records `SOURCE_REVISION`, `REDACTGUARD_BUILD_ID`, product version, Android API, emulator model and rendered display metrics next to every screenshot set. Emulator evidence is explicitly synthetic: it can support visual/adaptive review, but it cannot satisfy the physical-device accessibility, runtime-integration, performance or usability claim.

The seven retained surfaces are:

1. compact Document / Import;
2. compact Protection selection;
3. compact Analysis;
4. compact Review;
5. compact successful Outcome;
6. compact Recovery/Error;
7. expanded Review.

Exact-head visual evidence for the automated convergence gate passed on source revision `9a487ea286e97f8436594dbfd5e09b0d795937f6`. The retained artifact contains all seven required screenshots plus compact/expanded metadata and records the controlled API 35 rendering context. The screenshots were reviewed against `design/reference/README.md`; no clipping, blank surfaces, unsupported placeholder data or adaptive hierarchy regression was found.

## Emulator product-journey gate

`.github/workflows/emulator-e2e.yml` executes the three required `design/ux-contract.json` journeys on an API 35 emulator with synthetic content:

1. pasted text -> deterministic local analysis -> accepted review -> real PDF export -> isolated-parser reopen;
2. generated text PDF -> real isolated-parser import -> deterministic local analysis -> accepted review -> real PDF export -> reopen verification;
3. Local AI host unavailable -> canonical actionable recovery state -> retry -> successful analysis.

The LLM response boundary is deterministic so CI does not pretend an x86 emulator without an installed GGUF model proves native inference. Every other listed owner in those journeys is production code. The retained manifest identifies source revision, run, API and this claim boundary. Real Harness Binder/native/model execution remains a separate two-APK/physical-device gate.

Exact-head emulator product-journey E2E passed all three required journeys on source revision `9a487ea286e97f8436594dbfd5e09b0d795937f6`. Repository Validate and remote preflight also passed the FULL profile on that same candidate; the remote preflight exact-head identity check passed before the Android gates executed.

## Evidence policy

Repository validation proves formatting, compilation, semantics, tests, lint and packaging. Emulator visual evidence adds screenshot-backed reference/adaptive evidence. Emulator E2E adds deterministic product-journey evidence across ingestion, isolated PDF parsing, analysis orchestration, result validation, review/redaction, export and reopen. All automated evidence is bounded, retained for seven days and tied to the exact source revision.

VUI-2 through VUI-6 are DONE because repository validation, visual evidence and emulator product-journey E2E passed on the same reviewable source revision. VUI-7 is now ACTIVE: its automated prerequisites are satisfied, but named physical-device accessibility and real two-APK checks have not yet been recorded. The PR remains draft while that stronger physical-device completion gate is open.

When the migration-branch formatter creates a bot-authored formatting commit, that commit is treated only as a mechanical normalization step. A human-authored follow-up must sit on top before exact-head validation evidence is accepted, so formatting, build, tests and emulator evidence all address the same reviewable candidate revision.
