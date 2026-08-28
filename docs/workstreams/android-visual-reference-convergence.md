# RedactGuard Android Visual Reference Convergence

Status: active
Document type: workstream
Owner: redactguard-android
Read when: implementing or validating RedactGuard Android visual fidelity
Reference approved: 2026-08-26

## Goal

Converge the shipped RedactGuard Android five-step journey on the user-approved visual target with recognizably close composition, hierarchy, card system, semantic color, iconography, illustration treatment and adaptive review, while preserving truthful product state, Android-native interaction, accessibility, privacy boundaries and Harness runtime ownership.

The previous convergence wave proved the task model, semantics, emulator journeys and evidence plumbing, but its visual gate was too weak: it could pass a screen that was functionally correct yet visibly farther from the approved target than intended. This correction wave treats visual fidelity itself as an acceptance criterion.

## Canonical inputs

- `design/ux-contract.json` owns task semantics, state/recovery and privacy behavior.
- `design/brand-kit.json` owns tokens, brand assets and category colors.
- `design/reference/README.md` owns target adaptations/exclusions.
- the approved 1536x1024 visual target supplied by the user is the composition baseline; VUI-8 must store it in-repo with SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a` before visual implementation is accepted.
- production Compose remains executable truth; reference imagery never overrides a privacy/runtime invariant.

## Non-goals

- no OCR/VLM or image-only PDF support;
- no persisted history or bottom navigation merely because the mockup shows them;
- no generic Options/Settings destination without an owned product job;
- no cloud fallback;
- no Harness/model/runtime administration in normal RedactGuard UI;
- no fabricated percentage, occurrence count, category count, filename, page count or export metadata;
- no exact PDF-coordinate preview until the product owns that capability;
- no decorative motion/graphics required to understand or operate the task;
- no redesign of the underlying privacy task or Binder ownership.

## Invariants

- Document -> protection -> local analysis -> review -> export/recovery remains the critical journey.
- One primary decision/CTA per surface remains visually dominant.
- Sensitive values remain hidden by default and process-local.
- Any summary rail/outcome metric must come from existing process-local document/review/export state; synthetic values are allowed only inside clearly synthetic test fixtures.
- Analysis progress is derived only from real deterministic work units; otherwise show phases/indeterminate state.
- PII semantic color supplements text/icon/selection semantics and is never the only signal.
- Large text may simplify/reflow the composition; fidelity never overrides readability or reachable actions.
- Physical-device evidence is not interchangeable with emulator/CI evidence.

## Fidelity rule

For target-backed surfaces classify every visible element as:

1. **MATCH** — reproduce composition and visual role closely unless Android/accessibility constraints require a bounded adjustment;
2. **ADAPT** — preserve the target hierarchy but use truthful/native product behavior;
3. **EXCLUDE** — omit unsupported IA/data rather than fabricating it.

The first correction-wave merge requires side-by-side target vs emulator review. Pixel similarity may be advisory, but cannot replace composition/hierarchy review because the target is an illustrative mockup rather than an Android golden image. After acceptance, production screenshots become regression goldens while the approved target remains the fidelity owner.

## Work graph

| ID | Work | Owns / writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| VUI-1..6 | Original semantic/automation convergence | existing production/evidence | — | — | DONE |
| VUI-8 | Canonical target asset + explicit fidelity rubric/crops | `design/reference/**`, reference-only metadata | — | no | READY |
| VUI-9 | Shared visual system/shell fidelity | `RedactGuardScreens.kt`, `RedactGuardVisualPrimitives.kt`, theme/tokens | VUI-8 | yes | BLOCKED |
| VUI-10 | Repository-owned illustration/icon set | `app/src/main/res/drawable*` visual assets only | VUI-8 | yes | BLOCKED |
| VUI-15 | Truthful document/review/export summary projection | `ProductFlowState.kt`, `ProductUiModels.kt`, `RedactGuardProductViewModel.kt`, `MainActivity.kt`, related projection tests, `design/ux-contract.json` only if required | VUI-8 | yes | BLOCKED |
| VUI-11 | Document + Analysis target fidelity | `DocumentProtectionScreens.kt` + dedicated tests | VUI-9,VUI-10 | yes | BLOCKED |
| VUI-12 | Protection target fidelity | `ProtectionSelectionScreen.kt` + dedicated tests | VUI-9,VUI-10 | yes | BLOCKED |
| VUI-13 | Review compact + adaptive fidelity | `ReviewScreen.kt` + dedicated tests | VUI-9,VUI-10,VUI-15 | yes | BLOCKED |
| VUI-14 | Outcome + Recovery fidelity | `ProductFlowScreens.kt` + dedicated tests | VUI-9,VUI-10,VUI-15 | yes | BLOCKED |
| VUI-16 | Target-comparison Visual Evidence gate | `VisualReferenceEvidenceInstrumentationTest.kt`, visual capture/comparison scripts, `visual-evidence.yml` | VUI-11,VUI-12,VUI-13,VUI-14 | yes | BLOCKED |
| VUI-17 | True-journey screenshot/E2E reconvergence | `ProductJourneyUiEvidenceInstrumentationTest.kt`, E2E evidence collector/workflow | VUI-11,VUI-12,VUI-13,VUI-14 | yes | BLOCKED |
| VUI-18 | Exact-head integration/preflight | complete diff + selector-selected repository gates | VUI-16,VUI-17 | no | BLOCKED |
| VUI-7 | Physical accessibility/device evidence from `dev` | bounded named-device evidence only | VUI-18 | no | BLOCKED |

Parallel slices may not edit another slice's owner. Shared visual primitives land before screen slices. Summary projection is the explicit integration point for truthful counts/document metadata needed by expanded Review and Outcome.

## Current executable slice

`VUI-8` — canonical target asset + fidelity rubric.

Acceptance:

- store the exact approved target image under `design/reference/` and record dimensions/hash;
- store/codify target crops for Document, Protection, Analysis, compact Review, Outcome and expanded Review; Recovery remains contract/reference-style because the supplied composite has no dedicated Recovery panel;
- update `design/reference/README.md` with a per-surface MATCH / ADAPT / EXCLUDE matrix;
- no production behavior changes in this slice.

Validation:

- repository/documentation guards;
- verify binary hash and crop provenance;
- expected depth `LEAN`, selector remains authoritative.

## Surface acceptance

### Document / Import — VUI-11

MATCH:

- compact branded top bar, document-first hero, large headline and right-side shield/document visual;
- green local-AI-ready status treatment;
- two large input cards with primary PDF emphasis and secondary pasted text;
- cool white/grey layering, restrained border/elevation and target-like spacing.

ADAPT:

- no fake Settings destination in the top bar;
- local-only/support note remains visible but subordinate;
- image-only PDF constraint remains explicit without dominating the hero.

Acceptance:

- first-glance composition reads like target Screen 1, not a generic settings/form screen;
- primary PDF path is visually dominant and reachable at large text;
- illustration can disappear/reflow at extreme text scale without losing meaning.

### Protection — VUI-12

MATCH:

- `Cosa vuoi proteggere?` hierarchy;
- 2x2 recommended profile grid on compact portrait when width/text scale permits;
- clear selected card with border/check/text, not color alone;
- `Categorie selezionate` rows with category icon/tint, label/description and switch;
- full-width `Analizza in locale` as dominant exit action.

ADAPT:

- custom PII and consumer-safe preset choice become contextual/progressive-disclosure controls instead of competing with the primary decision;
- profile/category copy stays grounded in actual definitions.

Acceptance:

- target Screen 2 is recognizable without hiding current product capability;
- advanced/runtime-safe options do not visually turn the screen into an admin panel.

### Analysis — VUI-11

MATCH:

- centered analysis shield/status graphic;
- strong `Analisi in corso` title and concise local-processing explanation;
- vertical phase/progress treatment matching prepared -> searching -> validating hierarchy;
- secondary cancellation anchored below the primary processing surface.

ADAPT:

- page/percentage progress appears only if `AnalysisProgressModel` exposes deterministic work-unit progress;
- the current explanatory paragraph about why percentage is absent moves out of the main hierarchy; diagnostics/help may explain it when useful.

Acceptance:

- target Screen 3 is recognizable at a glance;
- no fabricated progress and no loss of cancellation semantics.

### Review compact — VUI-13

MATCH:

- centered Review title/progress hierarchy and prominent progress bar;
- category chip before finding content;
- `Possibile dato sensibile` + masked context card with highlighted focus placeholder;
- separate hidden-by-default detected-value surface with reveal affordance;
- `Oscura (consigliato)` full-width primary, `Mantieni` secondary;
- previous/next navigation visually subordinate to the decision.

ADAPT:

- `Vedi nel documento` is omitted until exact preview has an owned navigation capability;
- current category names map to the six visual families without changing domain semantics.

Acceptance:

- target Screen 4 action/context hierarchy is preserved;
- the current occurrence can be decided without scrolling past unrelated diagnostics/configuration.

### Review expanded / tablet — VUI-13 + VUI-15

MATCH:

- three-zone composition when sufficient width exists: summary/context rail, document context, decision pane;
- additional width exposes context and decision simultaneously rather than stretching a phone column.

ADAPT:

- left rail shows only truthful process-local projections: document label/page count where safely available, review progress and category counts derived from actual findings;
- if a projection is unavailable, remove that row rather than substitute placeholder metrics.

Acceptance:

- expanded composition is recognizably close to the target tablet panel;
- sensitive values remain hidden by default and no horizontal scrolling is required for primary actions.

### Outcome / Export — VUI-14 + VUI-15

MATCH:

- centered green completion/check motif;
- strong `Documento protetto` outcome and concise completion copy;
- summary/file cards and primary/secondary next actions arranged like the target when truthful data exists.

ADAPT:

- counts are derived from the process-local review set before it is cleared/reset;
- exported file label uses a truthful product-known name only; destination URI/path is not exposed unnecessarily;
- sharing action appears only if a real product flow owns it. Otherwise keep export verification/new-document actions.

Acceptance:

- target Screen 5 feels like a meaningful protected-document outcome rather than a generic success card;
- zero fabricated metrics or persistent sensitive state.

### Recovery — VUI-14

The supplied target does not include a dedicated recovery panel. Preserve current cause/action semantics, but visually use the same card, icon, spacing and button hierarchy as the target system. Diagnostics remain collapsed and privacy-safe.

## Shared system acceptance — VUI-9

- primary `#004AC6`, success/accent `#00B894`, six semantic PII families stay canonical;
- target radius family 8/12/16/24 dp and 0-3 dp restrained elevation;
- remove overuse of one giant `ProductPanel`; surfaces should compose cards/sections like the target;
- typography follows target hierarchy using repository-approved sans fallback until Inter is legitimately repository-owned;
- top bars support entry vs task-step variants without inventing navigation destinations;
- buttons, action cards, semantic tags, phase rows and value/context cards have reusable semantic primitives;
- touch targets, semantics, contrast and large-text behavior remain valid.

## Graphics acceptance — VUI-10

Repository-owned vectors/Compose graphics provide:

- document + shield hero motif;
- local-analysis shield/pulse motif;
- protected-document success ring/check motif;
- six category icons aligned to Identity, Contact, Health, Financial, Location, Other.

Graphics reinforce state/brand only. Functional meaning remains present in text/semantics when graphics are hidden or unavailable.

## Truthful summary projection — VUI-15

Add only process-local UI projections required by the target:

- review total/current position and accepted/ignored counts derived from `reviewOccurrences`;
- category-family counts derived from actual findings;
- document page count from the extracted descriptor;
- user-visible document/export label only when truthfully available and safe for normal UI; never include it in diagnostics/artifact metadata;
- export summary captured before the current state resets review counters.

Projection objects must have privacy-safe `toString()` behavior and must not introduce persistence.

## Visual Evidence v2 — VUI-16

The current gate only proves that screenshots exist. Replace that completion claim with target-fidelity evidence:

- retain the seven stable surfaces and source/build/environment metadata;
- package target crop + actual screenshot side-by-side in a generated HTML/contact sheet;
- assert distinctive semantic markers and required visual structure before capture;
- add deterministic geometry/component guards where practical (e.g. profile grid, action hierarchy, expanded pane count);
- optionally compute a perceptual-diff score as advisory telemetry; do not use a brittle pixel threshold as the sole target-fidelity gate;
- require explicit visual review of the side-by-side artifact before the first correction-wave merge;
- once accepted, retain production screenshot goldens/regression thresholds to detect future drift.

## E2E evidence v2 — VUI-17

Preserve the real product-journey assertions and the 14 checkpoint screenshots. Update checkpoint fixtures/markers so every journey renders the final production composition. The E2E gate continues to prove journey behavior; it does not replace target-comparison review.

Required journeys remain:

1. pasted text -> protection -> deterministic local analysis -> review -> real export -> reopen;
2. generated text PDF -> isolated parser -> protection -> analysis -> review -> export -> reopen;
3. Local AI unavailable -> actionable recovery -> retry -> success.

## Integration / validation — VUI-18

Before integration:

- refresh `dev` and review the complete diff;
- run the repository selector with `profile=auto`; never silently downgrade;
- expected contained UI slices are SCOPED, state/projection or AndroidTest/workflow changes may escalate to STRONG/FULL according to the selector;
- Android/Gradle gates unavailable agent-local are `REMOTE_AUTOMATED` and run through repository-owned Actions/preflight;
- require exact-head Visual Evidence v2 and E2E v2 artifacts;
- visually inspect the final target-vs-actual artifact, including compact and expanded surfaces;
- merge to `dev` only after deterministic gates and target-fidelity acceptance agree.

## Physical gate — VUI-7

Run from the integrated `dev` candidate after VUI-18:

- TalkBack and focus order;
- large text;
- compact landscape/adaptive behavior;
- representative OEM launcher rendering;
- real same-signer Harness + RedactGuard two-APK ARM64 journey;
- pasted text + text PDF + request-time PII definitions + runtime readiness;
- review, cancellation/recovery, Host absence/death/reconnect, export/reopen and cleanup.

VUI-7 remains REAL_ENVIRONMENT evidence and does not block parallel implementation of the fidelity wave, but testing the old visual candidate would be wasteful; therefore it is BLOCKED until VUI-18.

## Durable documentation destinations

- `design/reference/README.md`: final MATCH / ADAPT / EXCLUDE rules and target asset identity.
- `design/brand-kit.json`: only durable token/asset changes.
- `design/ux-contract.json`: only truthful new summary projections/behavior if the experience contract materially changes.
- instrumentation/component tests: executable visual/state semantics.
- `docs/current-state.md`: current integrated status and remaining physical evidence after each integration milestone.

## Completion

This workstream is complete only when the target-backed compact/expanded UI is recognizably aligned, deterministic repository/E2E gates pass on exact head, the fidelity artifact has been reviewed, VUI-7 physical evidence is recorded, durable contracts reflect reality and no unsupported target feature was fabricated. Then transfer durable truth and delete this workstream by default.