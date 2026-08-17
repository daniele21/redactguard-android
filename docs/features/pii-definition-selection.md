# PII definition selection

Status: migration in progress
Owner: RedactGuard

Built-in and custom PII definitions are product-owned. Selection state is process-local and begins with the versioned built-in definition set present but unselected.

Toggling a built-in changes only the active selection. Adding a custom PII definition delegates validation and limits to `PiiDefinitionFactory`; a valid custom definition is appended and selected immediately, while invalid input leaves state unchanged. Reset discards every transient custom definition and selection and returns to the built-in set.

This controller has no Android, Harness, Binder or persistence dependency. The ViewModel may project it into Compose models, but process recreation intentionally starts from a fresh selection state.
