# RedactGuard product design contract

This directory is the durable source of truth for RedactGuard product-experience semantics because the repository adopts the `product-ui` profile from `daniele21/repo-template-sw`.

Canonical owners:

- `ux-contract.json` — user outcome, task model, critical journey, action hierarchy, progressive disclosure, states, adaptive behavior, accessibility, motion purpose and evidence requirements;
- `brand-kit.json` — semantic visual language and motion tokens. It is introduced by the design-system implementation slice and must remain the only visual-token contract;
- `reference/` — a bounded set of key reference-view descriptions or intentionally retained views. It is not screenshot history;
- `app/src/main/kotlin/io/github/daniele21/redactguard/ui` — code-first implementation owner for Compose surfaces and canonical UI primitives.

## Decision order

Meaningful UX/UI work follows:

```text
user outcome
-> task model
-> information architecture / critical journey
-> information + action hierarchy
-> progressive disclosure / defaults
-> interactions + states + feedback + recovery
-> adaptive / platform behavior
-> accessibility
-> design system / components
-> motion
-> visual polish / graphics
-> validation
```

Do not use visual polish or animation to compensate for an unresolved earlier layer.

## RedactGuard-specific constraints

- Normal product surfaces describe the user's privacy task, not Harness/Binder/runtime implementation details.
- Sensitive finding values are hidden by default and revealed only by explicit user action.
- Technical diagnostics are secondary and progressively disclosed.
- Document text, findings, review decisions and reveal state remain process-local by default.
- There is no silent cloud fallback and no implicit OCR path for image-only PDFs.
- Reference screenshots, traces and videos used for regression evidence belong in bounded CI/evidence storage rather than accumulating here.

If implementation and this contract disagree, resolve the inconsistency explicitly; do not silently create a second design source of truth.
