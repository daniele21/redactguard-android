# Product UI boundary

Status: migration in progress
Owner: RedactGuard

RedactGuard owns its Compose screens and presentation models. Harnex `ui:design-system` is not a product dependency and must not be published merely to support this application.

The product shell has three top-level destinations: `Analizza`, `AI locale`, and `Impostazioni`. Compact windows use a Material 3 bottom `NavigationBar`; medium and expanded windows use a `NavigationRail`. Top-level destination selection is non-sensitive UI state and does not own or reset the nested Analyze workflow.

`Analizza` retains the state-hoisted product journey Import -> Definitions -> Analysis -> Review -> Outcome/Recovery. Switching to another top-level destination does not cancel, restart, or duplicate the active analysis; the product analysis owner remains canonical for job identity and lifecycle.

`AI locale` is a passive consumer-safe setup/readiness surface. It exposes an essential setup verdict first, an explicit refresh/retry action, product-context details such as use case and preset, advanced resolved model/configuration metadata behind progressive disclosure, and bounded technical details only when they exist. Missing consumer-safe model/configuration evidence is displayed as unavailable rather than reconstructed locally.

A `COMPATIBLE` setup is presented only as compatible. It is never promoted to final `Ready to Analyze` by the passive surface: Analyze repeats the authoritative fail-closed inspection immediately before activation and before document content can enter inference. Opening or refreshing `AI locale` reuses read-only setup discovery and must never activate, prepare, load, retain, or switch a model.

Preset replacement remains truthful: when a previously selected preset is stale, the product may surface the existing replacement notice. When the selector has no product value because there is no human-readable choice, the selected consumer-safe preset identity remains visible in setup details instead of being shown as unselected.

Normal Local AI product copy stays in the user's task language. Binder, Consumer SDK, model-residency and Harnex implementation terminology remains diagnostic/architecture language rather than normal task copy. Technical setup details are collapsed by default and remain limited to privacy-safe identifiers and revisions.

## Settings ownership

`Impostazioni` owns RedactGuard product preferences and explanatory product boundaries. It is not a Harnex administration or model/runtime configuration surface.

The analysis prompt is a RedactGuard-owned advanced product preference. Settings shows a readable preview and a clear `Predefinito`/`Personalizzato` state without forcing the user into the editor. `Visualizza e modifica` opens a dedicated full-screen editor on compact Android windows so long-form text remains usable with the software keyboard and large text.

The editor follows an explicit draft model: `Annulla` discards draft changes, `Salva` persists only a valid changed prompt, and `Ripristina predefinito` changes the draft first rather than performing an immediate destructive reset. Validation is inline and includes a character counter. The UI explains that prompt changes apply from the next analysis, while a running job keeps the instruction snapshot captured at start.

Protected integrity rules are shown as non-editable progressive disclosure. They remain active even with custom guidance and explain the key trust invariants without exposing Binder/Harnex internals. Users are warned not to put document contents, passwords or secrets into a prompt because a custom prompt is intentionally stored as an app-private durable preference.

The prompt editor must remain usable in compact portrait/landscape, medium/expanded windows, software-keyboard contexts and large text. Save/reset state cannot rely on color alone; labels, semantics and inline error text carry the meaning.

External Binder connection state is projected into a RedactGuard-owned enum/model before reaching the Analyze UI. Review values are hidden by default. A revealed source value is ephemeral and redacted from normal diagnostic output. Export remains disabled until the application layer confirms all required occurrence decisions and overlap/conflict invariants.

Document text, pasted text, findings, revealed values and review decisions remain process-local by default. The explicit custom analysis prompt is the narrow exception: it is an app-private user preference and may be durably stored. Prompt contents are not included in ordinary diagnostics/logs. Persisting the selected top-level destination or setup disclosure toggles does not expand the document-data privacy boundary.

## Migration provenance

The original RG-4 slice was replayed directly on the integrated RedactGuard bootstrap baseline. LAS-04 adds the adaptive application shell; LAS-05 layers the passive Local AI setup/recovery experience on that shell while reusing the current setup projection and process-local analysis owner. The editable-prompt slice extends the existing Settings/product-analysis owners rather than introducing parallel runtime or Harnex configuration state. PascalCase Compose function names use a file-local ktlint suppression only for the standard function-naming rule; repository-wide formatting and lint policy remain unchanged.
