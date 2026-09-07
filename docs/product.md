# RedactGuard product

This document owns concise durable product truth for RedactGuard. It constrains product decisions; architecture, feature behavior, UX detail and implementation keep their existing owners.

## Mission

Help people reduce unnecessary exposure of sensitive information by detecting and reviewing PII locally before a document is shared, then exporting only the redacted result they explicitly approve.

## Primary users / consumers

- People handling PDFs or text that may contain sensitive personal information and who want a simple local-first review/redaction workflow before sharing.
- Privacy-conscious users who value explicit control over what is removed and do not want the original content silently sent to cloud AI services.

## Core problems / jobs

- Identify user-selected sensitive-information types in imported PDF/text without requiring the original content to leave the trusted local product boundary.
- Make findings understandable and reviewable so the user remains the final authority over what is redacted.
- Produce a protected export that fails closed when analysis/redaction prerequisites are not satisfied.
- Explain Local AI/Harnex readiness and failures without leaking the sensitive document into diagnostics.

## Value proposition

RedactGuard combines local AI detection with explicit human review: the user gets help finding sensitive information while retaining control of the final redaction decision and avoiding a silent cloud-processing path for the original document.

## Meaningful differentiation

- **Local-first sensitive analysis:** original document content stays local by default; there is no silent cloud parsing/inference fallback.
- **Human-controlled redaction:** model findings are candidates, not irreversible policy decisions; users confirm or reject them.
- **Separated AI infrastructure:** RedactGuard owns the document/PII/review/export problem while Harnex owns model/runtime administration and execution lifecycle.
- **Privacy-safe failure handling:** Local AI setup/recovery and diagnostics are actionable without requiring raw document or finding values in logs.

## Core outcomes

- A user can import/paste content, choose what to protect, run local analysis, review findings and export a redacted document through one understandable workflow.
- Sensitive values stay hidden by default during review and are exposed only when the user needs them to make a decision.
- Missing/unavailable/restarted Harnex fails truthfully with actionable recovery instead of fake success or silent alternative processing.
- The user can distinguish what RedactGuard protects from what remains outside its guarantees.

## Non-goals

- RedactGuard is not a compliance guarantee or certification system.
- RedactGuard does not own LLM models, GGUF files, `llama.cpp`, model selection/residency or Harnex administration.
- OCR and general VLM/document-understanding are outside the current product boundary.
- There is no silent cloud parsing or inference fallback.
- RedactGuard is not a general-purpose local chatbot, password manager or enterprise DLP platform.

## Product principles

- **Privacy first, but claims stay precise.** Reduce unnecessary exposure without representing probabilistic detection as guaranteed compliance.
- **Human authority over model output.** Findings remain reviewable candidates; the user controls final redaction/export.
- **Fail closed on protection-critical uncertainty.** Missing analysis/readiness must not be presented as a safe redacted result.
- **RedactGuard owns the user problem; Harnex owns local-AI infrastructure.** Do not duplicate model/runtime policy inside the app.
- **No silent fallback or sensitive logging.** Changes that weaken the trusted local boundary require an explicit product decision.
- **Progressive disclosure protects usability and privacy.** Sensitive values, advanced PII configuration and recovery detail appear only when needed.
- **Evidence strength matches the claim.** Emulator/two-APK evidence is not representative physical ARM64/GGUF/memory/thermal/OEM proof.

## Product quality attributes

| Attribute | Importance | Product promise / principle | Technical owner |
| --- | --- | --- | --- |
| Privacy / data minimization | critical | original content stays local by default; sensitive values are not required in normal diagnostics/logging | privacy/redaction domain, `docs/architecture.md`, `SECURITY.md` |
| Protection correctness | critical | export/redaction fails closed when required analysis/selection state is not valid | privacy/redaction domain + export owners |
| User control / explainability | critical | users can understand, confirm or reject findings before redaction | review domain/ViewModel/UI owners, `design/*` |
| Local-AI trust boundary | critical | Harnex absence/denial/restart remains explicit; no bypass or silent fallback | Harnex integration/Consumer boundary owners |
| Reliability / recovery | high | interrupted Local AI and document workflows expose actionable recoverable states | infrastructure + ViewModel/state owners |
| Accessibility / adaptive UX | high | the protection workflow remains understandable across supported Android contexts | `design/*`, Compose/UI owners |
| Performance | high | local analysis and review should remain responsive enough for the protection task without hiding real device limits | Harnex execution boundary + app orchestration/evidence owners |
| Compatibility | high | published Harnex Consumer contract and Android behavior remain explicit and tested | Harnex integration + package/release owners |

## Success signals

Use the least invasive evidence sufficient to understand product value; privacy-sensitive analytics are not required by default.

- **Acceptance:** import/paste → PII selection → local analysis → review → redaction/export and the relevant recovery journeys pass required automated/release evidence without privacy leaks.
- **Outcome:** representative users can complete the protection task, understand findings/recovery and export the intended minimized document without needing to understand model/runtime internals.
- **Product impact:** task success, lower setup/recovery friction, fewer protection-critical failures and credible repeated use are stronger signals than shipped feature count. User observation, dogfooding, support feedback and privacy-safe aggregate evidence are all valid when proportionate.

## Canonical product sources

- Product-development routing: `.engineering/product.json`
- Public identity/value/usage: `README.md`
- Architecture/trust/ownership: `docs/architecture.md`
- Current integrated/blocked/next truth: `docs/current-state.md`
- Durable feature behavior: `docs/features/`
- Product experience: `design/`
- Harnex/Consumer evidence: `docs/evidence/physical-two-apk.md` and owning integration docs
- Delivery/E2E routing: `.engineering/commands.json` and `.engineering/e2e.json`

Update this file only when durable mission, target users, owned problems, value/differentiation, product boundaries/principles, core outcomes or material quality promises change.
