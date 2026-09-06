---
name: preflight-change
description: Establish RedactGuard integration/release readiness using exact identity, affected docs, risk-selected gates and equivalent evidence reuse.
---
# Preflight Change
For a coherent outcome ready for `dev` or release: refresh exact head/tree/base; review the complete diff; make affected durable docs current; resolve risks -> gates -> profile and executor; select affected critical E2E from `.engineering/e2e.json`; reuse only equivalent trusted evidence; route missing deterministic work via `remote-preflight`.

At INTEGRATION all required automated gates and affected automated E2E must pass. Material UI/UX journeys require `FULL_MEDIA`; required physical/OEM confirmation is `DEFERRED_TO_RELEASE`. RELEASE additionally requires FULL release evidence and every applicable required real-environment confirmation.

On failure use `validate-change` diagnosis. Return the bounded reporting fields from `.engineering/commands.json`, including failed/pending gates and remaining release obligations.
