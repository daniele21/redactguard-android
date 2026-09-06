---
name: structured-change
description: Shape meaningful RedactGuard behavior around its owner, observable user outcome, privacy invariants and regression evidence.
---
# Structured Change
State the **observable outcome, canonical owner, invariants to preserve and proof of success**. Inspect material consumers/fakes/tests and establish failing evidence for reproducible bugs when practical. Resolve material ambiguity from current code/contracts first.

Search before adding state/configuration/policy. Extend the existing owner; keep privacy/redaction policy out of UI/adapters. Preserve sensitive-data lifecycle, cancellation/cleanup, failure/recovery, Harnex/Binder compatibility and package semantics. For material UI use `../design-product-experience/SKILL.md` and canonical design owners.

Use `../validate-change/SKILL.md` during implementation. `../preflight-change/SKILL.md` owns integration/release readiness; do not repeat publication ceremony after every edit.
