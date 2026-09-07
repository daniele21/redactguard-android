# RedactGuard UI agent brief

Use this as the bounded bootstrap for material product-UI work. The canonical product contract remains `design/ux-contract.json`; open the relevant sections and owning feature docs when the touched journey requires them.

## Product hierarchy

Design from user outcome -> task model -> information/action hierarchy -> progressive disclosure/defaults -> states/recovery -> adaptive/accessibility -> components -> polish/evidence.

Normal surfaces use the user's privacy-task language. Harnex may be named for connection/authorization recovery; Binder, Consumer SDK, signer and runtime internals stay in technical diagnostics. Strong defaults must keep ordinary analysis usable without advanced configuration.

Top-level destinations are `Analizza`, `AI locale`, and `Impostazioni`. Compact windows use bottom navigation; medium/expanded windows may use a navigation rail. Navigation never activates a model or implicitly cancels/restarts an analysis.

## Privacy and state

Document text, findings, revealed values and review decisions remain process-local by default. Durable preferences must be explicit, app-private and privacy-reviewed. Never log sensitive values or user-authored prompt contents.

Harnex connection state, Local AI readiness, configuration compatibility and ready-to-analyze are distinct states. Recovery actions must match the actual owner; RedactGuard must not duplicate Harnex model/preset/application administration.

## Settings and analysis prompt

The analysis prompt is RedactGuard-owned advanced configuration. Keep the robust default visible and usable without editing. Settings exposes a bounded preview plus `Predefinito`/`Personalizzato`, then progressively discloses a dedicated editor.

Prompt editing uses explicit draft semantics: Cancel discards, Save persists only valid changes, Reset changes the draft first. On compact Android the long-form editor must remain usable with keyboard, large text and portrait/landscape. Validation meaning cannot rely on color alone.

Protected integrity rules are visible but read-only. Custom prompt contents are app-private durable preferences, never ordinary diagnostics, and affect only future analyses; an active job keeps its start-time snapshot.

For prompt-specific implementation and evidence read `docs/features/analysis-prompt-settings.md`, `docs/features/analysis-protocol.md`, and `docs/features/product-ui.md`.

## Evidence

Use component/state tests for deterministic behavior and E2E for complete journeys. Material UI/UX integration requires repository-declared `FULL_MEDIA`; screenshots alone do not prove accessibility, recovery or interaction quality. Physical ARM64/GGUF/model behavior remains separate REAL_ENVIRONMENT evidence when required.
