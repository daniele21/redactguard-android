# Product UI boundary

Status: migration in progress
Owner: RedactGuard

RedactGuard owns its Compose screens and presentation models. Harness `ui:design-system` is not a product dependency and must not be published merely to support this application.

The product shell has three top-level destinations: `Analizza`, `AI locale`, and `Impostazioni`. Compact windows use a Material 3 bottom `NavigationBar`; medium and expanded windows use a `NavigationRail`. Top-level destination selection is non-sensitive UI state and does not own or reset the nested Analyze workflow.

`Analizza` retains the state-hoisted product journey Import -> Definitions -> Analysis -> Review -> Outcome/Recovery. Switching to another top-level destination does not cancel, restart, or duplicate the active analysis; the product analysis owner remains canonical for job identity and lifecycle.

`AI locale` is a passive consumer-safe setup/readiness surface. It exposes an essential setup verdict first, an explicit refresh/retry action, product-context details such as use case and preset, advanced resolved model/configuration metadata behind progressive disclosure, and bounded technical details only when they exist. Missing consumer-safe model/configuration evidence is displayed as unavailable rather than reconstructed locally.

A `COMPATIBLE` setup is presented only as compatible. It is never promoted to final `Ready to Analyze` by the passive surface: Analyze repeats the authoritative fail-closed inspection immediately before activation and before document content can enter inference. Opening or refreshing `AI locale` reuses read-only setup discovery and must never activate, prepare, load, retain, or switch a model.

Preset replacement remains truthful: when a previously selected preset is stale, the product may surface the existing replacement notice. When the selector has no product value because there is no human-readable choice, the selected consumer-safe preset identity remains visible in setup details instead of being shown as unselected.

Normal Local AI product copy stays in the user's task language. Binder, Consumer SDK, model-residency and Harness implementation terminology remains diagnostic/architecture language rather than normal task copy. Technical setup details are collapsed by default and remain limited to privacy-safe identifiers and revisions.

`Impostazioni` owns only RedactGuard product preferences and explanatory product boundaries. It is not a Harness administration or model/runtime configuration surface.

External Binder connection state is projected into a RedactGuard-owned enum/model before reaching the Analyze UI. Review values are hidden by default. A revealed source value is ephemeral and redacted from normal diagnostic output. Export remains disabled until the application layer confirms all required occurrence decisions and overlap/conflict invariants.

Document text, pasted text, prompts, findings, revealed values, and review decisions remain process-local by default. Persisting the selected top-level destination or setup disclosure toggles does not expand that privacy boundary.

## Migration provenance

The original RG-4 slice was replayed directly on the integrated RedactGuard bootstrap baseline. LAS-04 adds the adaptive application shell on `feature/local-ai-setup-readiness`; LAS-05 layers the passive Local AI setup/recovery experience on that shell while reusing the current setup projection and process-local analysis owner. PascalCase Compose function names use a file-local ktlint suppression only for the standard function-naming rule; repository-wide formatting and lint policy remain unchanged.
