# RedactGuard Android Visual Reference Convergence

Status: active
Document type: workstream
Owner: redactguard-android
Read when: implementing or validating RedactGuard Android visual fidelity

## Goal

Make the five-step Android journey recognizably close to the user-approved target in composition, hierarchy, cards, semantic color, iconography, graphics and adaptive Review while preserving truthful state, Android-native behavior, accessibility, privacy and Harness ownership.

The first wave proved semantics/E2E/evidence plumbing but its visual gate was too weak: screenshots could pass while still visibly drifting from the target. This wave makes target fidelity an explicit acceptance criterion.

## Inputs and invariants

- `design/ux-contract.json` owns task/state/privacy behavior.
- `design/brand-kit.json` owns tokens/assets/colors.
- `design/reference/README.md` owns visual adaptation rules.
- The approved 1536x1024 target must be stored by VUI-8 with SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`.
- Production Compose is executable truth; the target cannot override privacy/runtime/accessibility invariants.
- Sensitive values remain hidden-by-default/process-local; target metrics/filenames/pages are shown only when truthfully projected.
- No fabricated analysis percentage; PII color is never the only signal.
- Large text may simplify/reflow visual composition to preserve readability/actions.

Non-goals: OCR/VLM, cloud fallback, persisted History/bottom navigation, generic Options/Settings, Harness administration, fabricated metrics, exact PDF-coordinate preview, decorative graphics required for operation.

## Fidelity rule

Every target element is classified as **MATCH** (copy composition/visual role closely), **ADAPT** (keep hierarchy with truthful/native behavior), or **EXCLUDE** (omit unsupported IA/data). First-wave acceptance requires target-vs-actual side-by-side review; brittle pixel equality is not the sole gate. After acceptance, production screenshots become regression goldens.

## Work graph

| ID | Work | Owns/writes | Depends | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| VUI-1..6 | First semantic/automation wave | existing implementation/evidence | — | — | DONE |
| VUI-8 | Canonical target + fidelity rubric/crops | `design/reference/**` | — | no | READY |
| VUI-9 | Shared shell/components/tokens | `RedactGuardScreens.kt`, `RedactGuardVisualPrimitives.kt`, theme/tokens | VUI-8 | yes | BLOCKED |
| VUI-10 | Graphics/category icon assets | `app/src/main/res/drawable*` visual assets | VUI-8 | yes | BLOCKED |
| VUI-15 | Truthful summary projection | `ProductFlowState.kt`, `ProductUiModels.kt`, ViewModel, `MainActivity.kt`, projection tests | VUI-8 | yes | BLOCKED |
| VUI-11 | Document + Analysis fidelity | `DocumentProtectionScreens.kt` + dedicated tests | VUI-9,VUI-10 | yes | BLOCKED |
| VUI-12 | Protection fidelity | `ProtectionSelectionScreen.kt` + dedicated tests | VUI-9,VUI-10 | yes | BLOCKED |
| VUI-13 | Review compact/expanded fidelity | `ReviewScreen.kt` + dedicated tests | VUI-9,VUI-10,VUI-15 | yes | BLOCKED |
| VUI-14 | Outcome + Recovery fidelity | `ProductFlowScreens.kt` + dedicated tests | VUI-9,VUI-10,VUI-15 | yes | BLOCKED |
| VUI-16 | Target-comparison Visual Evidence v2 | visual instrumentation/capture/workflow | VUI-11..14 | yes | BLOCKED |
| VUI-17 | E2E screenshot reconvergence | journey UI evidence + E2E collector/workflow | VUI-11..14 | yes | BLOCKED |
| VUI-18 | Exact-head integration/preflight | complete diff + selector gates | VUI-16,VUI-17 | no | BLOCKED |
| VUI-7 | Physical device/accessibility evidence | bounded named-device evidence | VUI-18 | no | BLOCKED |

Shared primitives land before surface slices. VUI-15 is the only owner allowed to add truthful summary data needed by expanded Review/Outcome.

## Current executable slice

`VUI-8`.

Acceptance:
- commit exact target image + dimensions/hash and crops for Document, Protection, Analysis, compact Review, Outcome and expanded Review;
- Recovery stays contract/style-based because the supplied target has no dedicated Recovery panel;
- add a per-surface MATCH/ADAPT/EXCLUDE matrix to `design/reference/README.md`;
- no production behavior change.

Validation: docs/reference guards + asset hash/provenance; expected LEAN, selector authoritative.

## Surface fidelity matrix

| Surface | MATCH | ADAPT / EXCLUDE | Acceptance |
| --- | --- | --- | --- |
| Document | branded top bar; document-first hero; shield/document graphic; AI-ready status; large PDF + paste cards; target spacing/layers | no fake Settings; support/local-only copy subordinate | instantly recognizable as target Screen 1; PDF remains dominant; large text safe |
| Protection | title/subtitle; 2x2 profile grid where space permits; selected border/check/text; category rows with icon/tint/description/switch; full-width Analyze CTA | custom PII/runtime presets move to contextual disclosure | recognizable as Screen 2; advanced capability no longer dominates |
| Analysis | centered shield/status graphic; strong title; concise local copy; prepared/searching/validating phases; secondary cancel | percentage/page only from deterministic `AnalysisProgressModel`; verbose explanation leaves main hierarchy | recognizable as Screen 3 with no fabricated progress |
| Review compact | title/progress; category chip; finding/context card; highlighted masked focus; separate hidden value; `Oscura (consigliato)` primary; Keep secondary; prev/next | omit `Vedi nel documento` until preview navigation exists | Screen 4 hierarchy preserved; decision dominates scrolling/content |
| Review expanded | three-zone summary/context/decision composition | summary rail contains only real process-local document/page/review/category projections | close to target tablet; no fake metrics; sensitive value hidden |
| Outcome | green completion motif; strong `Documento protetto`; target-like summary/file/action grouping | counts/file labels only from real state; Share only if real flow exists | Screen 5 feels like a product outcome, not generic success; zero fabricated data |
| Recovery | target card/icon/button visual language | preserve cause-specific recovery + collapsed diagnostics | same visual system without weakening failure semantics |

## Shared system — VUI-9/VUI-10

- keep canonical `#004AC6`, success/accent `#00B894`, six PII families, 8/12/16/24 dp radii and restrained 0-3 dp elevation;
- reduce blanket use of one giant `ProductPanel`; compose target-like cards/sections;
- add entry/task-step top-bar variants without fake destinations;
- reusable semantic action cards, tags, phase rows, value/context cards and buttons;
- repository-owned document+shield hero, analysis shield/pulse, success ring/check and six category icons;
- graphics remain optional to comprehension and expose no sensitive data.

## Truthful summary projection — VUI-15

Only process-local projections may support target richness:
- current/total review plus redact/ignore counts from actual occurrences;
- six-family category counts from actual findings;
- page count from extracted descriptor;
- user-visible document/export label only when available for normal UI and never in diagnostics/evidence metadata;
- capture export summary before review counters are reset.

No persistence. Projection `toString()` remains privacy-safe. Update `design/ux-contract.json` only if this materially changes the durable experience contract.

## Evidence v2 — VUI-16/VUI-17

Visual Evidence must:
- retain seven stable surfaces + exact source/build/environment identity;
- package target crop and actual screenshot side-by-side in HTML/contact sheet;
- assert distinctive semantics and practical structural guards (e.g. profile grid/action hierarchy/expanded pane count);
- optionally report perceptual diff as advisory telemetry;
- require explicit target-vs-actual review for the first correction-wave merge, then protect accepted production goldens against drift.

E2E keeps the 14 asserted checkpoints and the existing real journey assertions:
1. pasted text -> protect -> local analysis -> review -> real export/reopen;
2. generated text PDF -> isolated parser -> protect -> analysis -> review -> export/reopen;
3. Local AI unavailable -> actionable recovery -> retry -> success.

## Parallel execution

1. VUI-8.
2. VUI-9 + VUI-10 + VUI-15 in parallel.
3. VUI-11 + VUI-12 + VUI-13 + VUI-14 in parallel after their dependencies.
4. VUI-16 + VUI-17 in parallel.
5. VUI-18 integration to `dev`.
6. VUI-7 physical evidence from corrected `dev`.

## Validation / completion

Each PR uses `profile=auto`; contained UI is expected SCOPED while shared state/AndroidTest/workflow changes may escalate. Required unavailable Android gates are REMOTE_AUTOMATED, never delegated to the user. VUI-18 requires refreshed `dev`, complete diff review, exact-head Visual Evidence v2, E2E v2 and selector-selected preflight.

VUI-7 then records TalkBack, large text, compact landscape/adaptive behavior, OEM launcher rendering and real same-signer Harness + RedactGuard ARM64 journeys including recovery/reconnect/export/cleanup.

Durable destinations: `design/reference/README.md`, `design/brand-kit.json` only for real token/assets, `design/ux-contract.json` only for material truthful projections, executable tests, and `docs/current-state.md`. Complete only when target fidelity, automated evidence and VUI-7 all agree; then transfer durable truth and delete this workstream by default.