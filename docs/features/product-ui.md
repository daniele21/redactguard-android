# Product UI boundary

Status: migration in progress
Owner: RedactGuard

RedactGuard owns its Compose screens and presentation models. Harness `ui:design-system` is not a product dependency and must not be published merely to support this application.

The initial state-hoisted screen inventory is Import -> Definitions -> Analysis -> Review. External Binder connection state is projected into a RedactGuard-owned enum/model before reaching UI, so Binder transport types do not leak into presentation code.

Review values are hidden by default. A revealed source value is ephemeral and redacted from normal diagnostic output. Export remains disabled until the application layer confirms all required occurrence decisions and overlap/conflict invariants.
