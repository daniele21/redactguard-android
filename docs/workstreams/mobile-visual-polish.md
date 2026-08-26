# Mobile visual polish

Status: active
Document type: workstream
Owner: redactguard-android
Goal: turn the already-settled RedactGuard mobile task model and interaction hierarchy into a visibly distinctive, production-grade Android product surface with screenshot-verifiable references, without changing runtime ownership or privacy boundaries.

## Non-goals

- no OCR/VLM or image-only PDF support;
- no persisted history or bottom navigation;
- no cloud fallback;
- no Harness/model/runtime administration inside RedactGuard;
- no exact PDF-coordinate preview;
- no decorative motion that changes or delays privacy/recovery actions.

## Completion rule

This workstream is not DONE merely because semantic hierarchy, tokens or Compose tests are green. Completion requires all of the following:

1. the stable product surfaces have explicit visual acceptance criteria;
2. the app shell and shared components render the RedactGuard brand instead of default Material scaffolding;
3. input, protection, analysis, review and outcome/recovery surfaces use the shared visual system consistently;
4. compact and wider Android layouts preserve the same hierarchy;
5. deterministic component/instrumentation tests cover semantic roles and state hierarchy;
6. representative screenshot/visual evidence exists for stable high-risk surfaces;
7. physical-device accessibility/adaptive evidence remains explicitly PENDING until executed on a named device.

## Visual direction

Use the existing `design/brand-kit.json` Quiet Professional direction: blue, private, document-centric, calm and professional. Prefer cool layered surfaces, clear borders, restrained elevation and branded document/shield motifs. Avoid generic Material demo composition, cyber/neon styling, decorative gradients that compete with the task and duplicated raw style values.

The normal phone experience should read as a coherent product:

```text
branded app shell
  -> task title + one clear primary action
  -> contextual privacy/local-AI status
  -> focused working surface
  -> restrained supporting information
```

## Visual acceptance criteria by surface

### Import

- compact RedactGuard mark/wordmark treatment in the app shell;
- document-protection hero surface, not a loose stack of text and buttons;
- `Importa PDF` is visually dominant;
- `Incolla testo` is a clearly secondary action;
- local-only/privacy explanation is visible but subordinate;
- unsupported image-only PDFs are contextual, not competing with the CTA.

### Protection selection

- protection profiles read as real decision cards with selected/unselected visual states;
- individual PII categories use a compact semantic selection treatment rather than a visually undifferentiated long chip list;
- selected profile/category state remains understandable without color alone;
- the analyze CTA is anchored and visually dominant.

### Analysis

- one focused progress surface communicates local processing;
- no fabricated percentage;
- cancellation is available but secondary;
- the screen should not look like a generic loading example.

### Review

- masked document context and the current decision are visually distinct;
- current occurrence/category/progress are easy to scan;
- reveal is subordinate to `Oscura` / `Mantieni`;
- redact vs keep actions have clear but non-alarming hierarchy;
- export remains unavailable until the review contract allows it;
- medium/expanded layouts use width for context + decision, compact stays single-focus.

### Outcomes and recovery

- success communicates completion and next step with a branded outcome surface;
- failure communicates cause and recovery first; diagnostics remain collapsed;
- status styling is semantic and readable without relying only on color.

## Parallel slices

| Slice | Status | Depends on | Owns / writes | Validation |
| --- | --- | --- | --- | --- |
| VUI-1 visual contract + completion gate | DONE | none | `docs/current-state.md`, this workstream, `design/reference/README.md` | documentation policy + review |
| VUI-2 shared app shell and semantic components | DONE | VUI-1 | shared shell/theme/component primitives | exact-head Kotlin/build + component semantics |
| VUI-3 import + analysis surfaces | DONE | VUI-2 | input/processing composables | exact-head Compose/build validation |
| VUI-4 protection surface | DONE | VUI-2 | profile/category selection composables | exact-head Compose/build validation |
| VUI-5 review + outcome/recovery surfaces | DONE | VUI-2 | review/outcome/error composables | exact-head adaptive + review validation |
| VUI-6 visual evidence | BLOCKED | VUI-3,VUI-4,VUI-5 | stable screenshot/reference evidence only | screenshot regression on explicit Android rendering target |
| VUI-7 physical-device UX evidence | BLOCKED | VUI-3,VUI-4,VUI-5 | evidence record only | TalkBack, large text, compact/landscape on named device |

VUI-2 through VUI-5 are implemented in draft PR #96. Their software validation passed on exact head `9a392071dc29d9298cd1580fd0009a4cf09d107f`: formatting, helper/failure contracts, app Kotlin, JVM test compilation/tests, AndroidTest APK assembly, Android Lint, debug APK and minified release APK all passed.

VUI-6 remains blocked until the stable surfaces are rendered on an explicit Android target and retained as build/source-identified visual evidence. VUI-7 remains blocked until named-device TalkBack, large-text and adaptive checks are actually executed. The workstream therefore remains active even though its software implementation slices are DONE.

## Evidence policy

Repository validation can prove buildability, semantics and deterministic component behavior. It cannot prove visual quality on a real handset. Screenshot evidence and named-device review are separate gates and must remain PENDING until actually recorded.
