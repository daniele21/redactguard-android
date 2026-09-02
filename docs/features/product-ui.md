# Product UI boundary

Status: migration in progress
Owner: RedactGuard

RedactGuard owns its Compose screens and presentation models. Harness `ui:design-system` is not a product dependency and must not be published merely to support this application.

The product shell has three top-level destinations: `Analizza`, `AI locale`, and `Impostazioni`. Compact windows use a Material 3 bottom `NavigationBar`; medium and expanded windows use a `NavigationRail`. Top-level destination selection is non-sensitive UI state and does not own or reset the nested Analyze workflow.

`Analizza` retains the state-hoisted product journey Import -> Definitions -> Analysis -> Review -> Outcome/Recovery. Switching to another top-level destination does not cancel, restart, or duplicate the active analysis; the product analysis owner remains canonical for job identity and lifecycle.

`AI locale` is a passive consumer-safe setup/readiness surface. It observes the existing setup projection and may show the selected product mode plus resolved consumer-safe model/configuration metadata, but navigating there must never activate, prepare, load, retain, or switch a model. Harness remains the owner of model/configuration/runtime state.

`Impostazioni` owns only RedactGuard product preferences and explanatory product boundaries. It is not a Harness administration or model/runtime configuration surface.

External Binder connection state is projected into a RedactGuard-owned enum/model before reaching the Analyze UI. Review values are hidden by default. A revealed source value is ephemeral and redacted from normal diagnostic output. Export remains disabled until the application layer confirms all required occurrence decisions and overlap/conflict invariants.

Document text, pasted text, prompts, findings, revealed values, and review decisions remain process-local by default. Persisting the selected top-level destination does not expand that privacy boundary.

## Migration provenance

The original RG-4 slice was replayed directly on the integrated RedactGuard bootstrap baseline. The LAS-04 navigation shell is stacked on `feature/local-ai-setup-readiness` so it consumes the current process-local analysis owner without duplicating lifecycle ownership. PascalCase Compose function names use a file-local ktlint suppression only for the standard function-naming rule; repository-wide formatting and lint policy remain unchanged.
