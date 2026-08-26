# RedactGuard Android visual reference baseline

Keep this directory intentionally small. The approved Android reference reviewed on 2026-08-26 is the visual composition baseline for the five-step journey `Document -> What to protect -> Analysis (local) -> Review findings -> Outcome / Export`, plus the adaptive review treatment. The desktop `daniele21/redact-guard` product remains the canonical brand ancestor; `design/ux-contract.json` remains the semantic owner.

The approved reference is not permission to copy UI that conflicts with the current product contract. The implementation must converge on its hierarchy, card system, icon treatment, semantic PII colors, restrained elevation and adaptive composition while preserving Android-native interaction and the privacy/runtime boundaries already defined in this repository.

## Reference decisions

Follow directly:

- primary `#004AC6` brand hierarchy and green `#00B894` accent/success language;
- white / cool-grey layered surfaces, quiet borders, low elevation and 8/12/16/24 dp radius family;
- compact branded RedactGuard top bar;
- document-first entry hero and large input action cards;
- profile cards before detailed protection customization;
- six visual PII families: Identity, Contact, Health, Financial, Location, Other;
- analysis as one focused local-processing surface;
- review progress + category + context + decision hierarchy;
- full-width primary redact action and clearly secondary keep action;
- strong but restrained protected-document outcome;
- expanded review that spends additional width on context + decision rather than stretching the phone layout.

Adapt rather than copy literally:

- `AI locale pronta` is a compact product status. Harness/Binder/model vocabulary remains diagnostic-only;
- an analysis percentage is allowed only when derived from deterministic real work units. Otherwise use truthful phases/indeterminate progress;
- the reference document preview is represented by masked source context until exact PDF-coordinate preview becomes an explicit requirement;
- the expanded reference's left summary rail is added only when the UI state owns truthful document/category summary data.

Do not implement from the reference in this workstream:

- bottom navigation;
- persisted `Cronologia`;
- a generic `Opzioni` destination without an owned product job;
- fabricated progress;
- cloud fallback;
- OCR/VLM or image-only PDF handling;
- exact PDF-coordinate preview.

## Import and input

The entry surface must make the privacy task obvious and keep one primary path. `Importa un PDF` is visually dominant and uses a document action card; `Incolla testo` is secondary. The RedactGuard mark can reinforce the protection task without becoming necessary to understand it. Local-only behavior and unsupported-input constraints are visible but subordinate.

Key states: default, importing, permission/source failure, image-only PDF recovery.

Required stable visual reference: compact-phone default import surface.

## Protection selection

Profiles are the primary decision model and read as a 2-column set of selectable cards on compact phones when space permits. Selected state uses border/indicator/text semantics in addition to color. Detailed PII definitions remain the customization layer and inherit one of the six reference semantic color families without changing the richer domain taxonomy.

Key states: default recommended profile, alternate profile selected, customized categories, local-AI unavailable, analyze disabled/enabled.

Required stable visual reference: compact-phone protection surface with one selected profile and representative categories.

## Analysis

Analysis uses one focused surface with the RedactGuard protection motif, a truthful current state, explicit phases and secondary cancellation. The current implementation can truthfully represent document preparation as completed, sensitive-data search as active and result validation as pending; it must not infer elapsed-time percentage.

Key states: runtime unavailable, analysis running, cancellation, transition to findings/no-findings.

Required stable visual reference: compact-phone analysis-running surface.

## Review

Review keeps current occurrence and redact/keep decision dominant, preserves hidden-by-default sensitive values and shows deterministic review progress. Category color is a scanning aid only. The source context highlights the masked focus placeholder rather than exposing the sensitive value. Compact remains single-focus; medium/expanded keeps source context and decision visible in parallel.

The approved adaptive mockup includes a richer left document/category rail. That rail is intentionally deferred until RedactGuard owns truthful filename/page/category-count projections; placeholder metrics must not be invented for visual similarity.

Key states: findings available, pending/redact/ignore decision, previous/next boundaries, export disabled/enabled.

Required stable visual references:

- compact-phone review surface;
- one medium/expanded review surface showing context + decision panes.

## Error, success and recovery

Success uses a centered protected-document outcome with the green accent and one clear next step. Counts, filename and share/export details are shown only when the product state owns them; the current post-export state therefore does not fabricate the reference's occurrence counters.

The error surface leads with user-understandable cause and recovery. Stable code, stage and operation ID remain behind explicit technical-detail disclosure. User content, prompts, findings and raw Binder/parser payloads never appear in diagnostics.

Key states: export success, classified input failure, local-AI dependency failure, export destination/write failure, unknown internal fallback.

Required stable visual references: export success and one representative recoverable error.

## Evidence policy

Visual evidence is a separate gate from semantic/component tests. The minimum stable screenshot set is:

1. Import default — compact phone.
2. Protection selection — compact phone.
3. Analysis running — compact phone.
4. Review — compact phone.
5. Review — medium/expanded Android window.
6. Export success — compact phone.
7. Recoverable error — compact phone.

Each retained artifact must identify exact source revision/build and rendering context. Screenshot comparison is against the approved reference hierarchy and this adaptation contract, not against unsupported features visible in the mockup. Screenshot evidence does not replace interaction, accessibility, adaptive or physical-device validation.
