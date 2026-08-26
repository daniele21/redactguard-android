# RedactGuard key reference views

Keep this directory intentionally small. These references define the product states whose hierarchy, semantics and visual identity must remain stable across implementation changes.

The prose below is the durable acceptance contract. Representative screenshots/visual-regression artifacts are required for the stable surfaces called out here before the visual-polish workstream may be declared complete. Generated run artifacts remain bounded evidence with build/source identity rather than becoming a second design source of truth.

## Import and input

The entry surface must make the privacy task obvious and keep one primary action. `Import PDF` is primary; `Paste text` is secondary. Supported-input constraints are contextual. Runtime implementation terminology is absent from the default view.

Visual identity requirements:

- branded RedactGuard app shell rather than a plain text title row;
- document/protection working surface that groups task title, privacy reassurance and input actions;
- clear separation between primary import action and secondary paste-text action;
- cool layered surfaces, restrained border/elevation and the canonical blue brand tokens;
- unsupported-input guidance visually subordinate to the task.

Key states: default, importing, permission/source failure, image-only PDF recovery.

Required stable visual reference: compact-phone default import surface.

## Protection selection

Profiles are the primary decision model and must read as selectable semantic cards rather than an undifferentiated settings list. Individual categories remain available as a compact customization layer. Selected state must be understandable without color alone and the analyze action remains dominant.

Key states: default recommended profile, alternate profile selected, customized categories, local-AI unavailable, analyze disabled/enabled.

Required stable visual reference: compact-phone protection surface with one selected profile and representative categories.

## Analysis and review

Analysis communicates truthful phases and offers cancellation without inventing percentage completion. It should present one focused local-processing surface rather than a generic loading example.

Review keeps the current occurrence and its redact/ignore decision dominant, preserves hidden-by-default sensitive values and shows review progress without overwhelming the finding itself. Context and decision must be visually distinct. Wider layouts may keep both visible in parallel; compact layout stays single-focus.

Key states: runtime unavailable, analysis running, cancellation, findings available, no findings, export disabled/enabled.

Required stable visual references:

- compact-phone analysis-running surface;
- compact-phone review surface;
- one medium/expanded review surface showing context + decision panes.

## Error, success and recovery

The error surface leads with the user-understandable cause and the correct recovery action. Stable code, stage and operation ID remain behind technical-detail disclosure. User content, prompts, findings and raw Binder/parser payloads never appear in diagnostics.

Success uses a branded outcome surface that clearly communicates completion and the next action without unnecessary celebration or decorative motion.

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

Each retained visual artifact must identify the exact source revision/build and rendering context. Screenshot evidence does not replace interaction, accessibility, adaptive or physical-device validation.
