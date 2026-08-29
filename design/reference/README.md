# RedactGuard Android visual reference baseline

The user-approved Android reference reviewed on 2026-08-26 is the visual-composition baseline for `Document -> What to protect -> Analysis (local) -> Review findings -> Outcome / Export` plus expanded Review. `design/ux-contract.json` remains the semantic owner and `design/brand-kit.json` remains the brand/token owner.

## Canonical target identity

The approved source is a 1536x1024 raster image whose exact source-byte identity is:

- SHA-256: `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`
- dimensions: `1536x1024`
- encoded bytes: JPEG (the original chat attachment used a `.png` display name, but its actual bytes are JPEG)
- approval date: `2026-08-26`
- repository source: `design/reference/approved-target.jpg`

`target-provenance.json` owns deterministic crop coordinates and checksum identity. Visual Evidence v2 must compare actual screenshots against these target regions, not against a remembered description of the image.

The light Android task header additionally uses `app/src/main/res/drawable-nodpi/redactguard_android_shield.png`, a lossless PNG re-encoding of the exact target pixels `(8,18)-(62,85)`. It is a target-derived crop, not a redrawn logo. Dark theme retains the canonical compact desktop mark because the approved crop includes the light reference background.

The reference cannot override privacy/runtime/accessibility invariants. Every visible reference element is classified as:

- **MATCH** — reproduce composition and visual role closely;
- **ADAPT** — preserve hierarchy while using truthful product state and Android-native behavior;
- **EXCLUDE** — omit unsupported navigation, data or capability.

## Surface fidelity matrix

| Surface | MATCH | ADAPT | EXCLUDE |
| --- | --- | --- | --- |
| Document | compact branded header; document-first hero; shield/document illustration; ready status; large PDF and paste action cards; cool layered surfaces | privacy/support copy stays subordinate; PDF remains dominant | fake Settings destination; bottom navigation |
| Protection | title/subtitle; 2x2 recommended profile cards where width permits; selected border/check/text; semantic category icon/tint/description/switch; full-width Analyze CTA | custom PII and multiple runtime presets stay contextual; large text may collapse grid | fabricated category counts; internal Harness/model administration |
| Analysis | centered protection graphic; strong title; concise local copy; prepared/searching/validating phases; secondary cancel | percentage/page only when derived from deterministic work units; runtime preparation copy may refine active phase | fabricated elapsed-time percentage; cloud fallback |
| Review compact | title/progress; semantic category chip; finding/context card; highlighted masked focus; separate hidden value; dominant `Oscura (consigliato)`; secondary Keep; previous/next | context remains masked rather than a coordinate-accurate PDF preview; export remains disabled until every decision is complete | `Vedi nel documento` until real preview navigation exists |
| Review expanded | three-zone summary/context/decision composition; category scanability; decision pane always obvious | summary rail uses only truthful process-local document/page/review/category projections; first acceptance capture is true tablet landscape | placeholder filename/pages/counts; persistent history |
| Outcome | green completion motif; strong `Documento protetto`; target-like summary/file/action grouping | counts and file label only from real process-local state; current screen is post-export so `Nuovo documento` is the real next action | fabricated 18/12/6-style metrics; fake share action |
| Recovery | same card/icon/button visual language as the five-step journey | cause-specific recovery and collapsed privacy-safe diagnostics remain authoritative | generic error copy that hides a known cause |

## Deterministic target regions

Visual Evidence v2 uses these source-image rectangles (`left,top,right,bottom`, pixels):

1. Document: `270,0,500,610`
2. Protection: `495,0,735,610`
3. Analysis: `735,0,985,610`
4. Review compact: `978,0,1242,610`
5. Outcome: `1236,0,1536,610`
6. Review expanded: `900,615,1536,1024`

Recovery has no dedicated panel in the supplied target and therefore follows the shared visual system plus the product failure contract rather than a fabricated crop.

## Shared visual decisions

Follow directly:

- primary `#004AC6` hierarchy and green `#00B894` accent/success language;
- white/cool-grey layered surfaces, quiet borders, low elevation and 8/12/16/24 dp radius family;
- compact RedactGuard brand treatment;
- document-first entry hierarchy and large input actions;
- profile cards before detailed protection customization;
- six visual PII families: Identity, Contact, Health, Financial, Location, Other;
- focused local-analysis composition;
- review progress -> category -> context -> value -> decision hierarchy;
- full-width redact primary action and clearly secondary keep action;
- strong but restrained protected-document completion;
- expanded Review spends width on summary/context/decision instead of stretching the phone column.

Adapt rather than copy literally:

- `AI locale pronta` is a compact product status; Harness/Binder/model vocabulary remains diagnostic-only;
- analysis progress is numeric only when deterministic real work units exist;
- source context remains masked until exact PDF-coordinate preview becomes an owned requirement;
- expanded summary data is shown only when `ProductSummaryProjector` can derive it from process-local product state;
- Review keeps export unavailable until every occurrence has a decision, even though the target board emphasizes the per-occurrence decision only;
- Outcome is already post-export in the current flow, so it must not fabricate a second Share/Export action;
- large text and compact landscape may reflow cards/panes to preserve reachability and reading order.

Do not implement from the target:

- bottom navigation or persisted `Cronologia`;
- generic `Opzioni`/Settings without an owned job;
- fabricated progress, counts, filenames or page metrics;
- fake `Condividi`/Share capability;
- cloud fallback;
- OCR/VLM/image-only PDF handling;
- exact PDF-coordinate preview or `Vedi nel documento` without real navigation ownership.

## Per-surface acceptance

### Document

The first viewport should be immediately recognizable as target Screen 1: brand, privacy job, protection graphic, local-ready status and two large input actions. `Importa un PDF` remains the clear primary route. Unsupported-input guidance is visible but does not compete with the job.

### Protection

Recommended profiles form the first decision layer and use a 2-column grid on normal compact phones. Selected state is non-color-only. Category rows use the six-family visual language. Custom definitions and multiple consumer-safe runtime modes remain discoverable but must not visually dominate the default path. Visual evidence must show a canonical profile only when its selected definitions exactly match that profile.

### Analysis

The main viewport is a centered local-protection state with one protection graphic, truthful active phase, the three user-understandable phases and a secondary cancel action. Verbose runtime explanation does not displace the task hierarchy.

### Review

Compact Review remains single-focus and decision-first. Sensitive values stay hidden by default and reveal remains an explicit accessible action. Expanded Review uses truthful summary/context/decision zones when width permits; no richer rail is allowed unless the state owns the data. The VUI-18 acceptance capture uses a `2560x1600@320dpi` landscape emulator window so the adaptive target is evaluated in the orientation it actually specifies.

### Outcome / Recovery

Outcome must feel like a product completion state rather than a generic success card. Any counts or document identity are real product projections. A completed Outcome fixture must satisfy `total == redacted + kept` and `pending == 0`. Recovery uses the same visual language but never weakens the cause-specific error/retry contract.

## Evidence policy

The stable evidence set remains:

1. Import default — compact phone.
2. Protection selection — compact phone with a genuinely matching canonical profile selected.
3. Analysis running — compact phone.
4. Review — compact phone with privacy-safe realistic synthetic context.
5. Review — expanded Android window, true landscape `2560x1600@320dpi`.
6. Export success — compact phone with a completed internally coherent synthetic summary.
7. Recoverable error — compact phone.

The first fidelity-correction candidate additionally packages target region + actual screenshot side-by-side and receives explicit visual review. Structural assertions protect hierarchy; perceptual distance may be reported as advisory telemetry but is not the sole gate. After acceptance, the production screenshots become regression goldens.

Every artifact identifies exact source revision/build/rendering context. Screenshot evidence does not replace interaction, accessibility, adaptive or physical-device validation.
