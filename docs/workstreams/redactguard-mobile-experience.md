# RedactGuard mobile experience

Status: active
Owner: redactguard-android
Read when: implementing or coordinating the RedactGuard Android UX/UI convergence with the desktop brand and expanded PII taxonomy.

## Goal

Deliver a production-ready Android experience for local document protection that preserves the existing critical journey while converging the RedactGuard desktop brand, expanding the product-owned PII taxonomy/profiles, making review context-first, and preserving privacy-safe adaptive/accessibility behavior.

## Non-goals

- OCR/VLM or image-only PDF support.
- Cloud parsing/fallback.
- Runtime/model ownership or Harness administration inside RedactGuard.
- Persisted document/finding history or bottom navigation before a separately privacy-reviewed History capability exists.
- Exact PDF coordinate highlighting in this wave; review uses text context with the sensitive span masked by default.

## Invariants

- RedactGuard owns PII types, descriptions, profile membership and review policy; Harness owns model/runtime/preset execution.
- Selected PII definitions are sent as bounded request metadata through the published Consumer SDK; Harness may embed them into the effective system prompt but must not become the taxonomy source of truth.
- Sensitive document text, raw finding values and review decisions remain process-local by default and are not logged/persisted.
- Finding values remain hidden by default; review context may expose only the minimum surrounding text needed for the decision and masks the finding span until explicit reveal.
- No fabricated progress percentage; determinate progress is shown only when real bounded work units are available.
- Normal UI uses task language. Harness/Binder/model/runtime identity remains progressively disclosed diagnostics only.
- Existing host-published inference preset selection remains distinct from RedactGuard PII profiles.

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| RGUX-1 | Brand convergence and semantic token contract | `design/brand-kit.json`, `ui/theme/**`, brand drawable/resource assets | — | yes | READY |
| RGUX-2 | Expanded desktop-aligned PII taxonomy and product profiles | `domain/pii/**`, taxonomy/profile tests | — | yes | READY |
| RGUX-3 | Consumer request contract for PII definitions | RedactGuard Harness adapter + direct tests; external dependency: Harness Consumer SDK contract | RGUX-2 | yes | BLOCKED |
| RGUX-4 | Canonical mobile component system | `ui/components/**` and shared UI semantics | RGUX-1 | yes | BLOCKED |
| RGUX-5 | Document/input screen convergence | input composables + UI tests | RGUX-4 | yes | BLOCKED |
| RGUX-6 | Protection profiles/categories experience | protection composables/state + tests | RGUX-2, RGUX-4 | yes | BLOCKED |
| RGUX-7 | Truthful local-analysis experience | analysis composables/state + tests | RGUX-4 | yes | BLOCKED |
| RGUX-8 | Privacy-safe review context projection | review projection/models + tests | RGUX-2 | yes | BLOCKED |
| RGUX-9 | Phone review workspace | review composables + UI tests | RGUX-4, RGUX-8 | yes | BLOCKED |
| RGUX-10 | Outcome/export visual experience | export/outcome composables + tests | RGUX-4 | yes | BLOCKED |
| RGUX-11 | Expanded/landscape review workspace | adaptive review layout + tests | RGUX-8, RGUX-9 | yes | BLOCKED |
| RGUX-12 | Motion, accessibility and visual regression | motion tokens, semantics, screenshot/accessibility evidence | RGUX-5, RGUX-6, RGUX-7, RGUX-9, RGUX-10, RGUX-11 | yes | BLOCKED |
| RGUX-13 | Cross-repo integration and final validation | Consumer SDK version alignment, repository gates, device evidence | RGUX-3, RGUX-12 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Current executable slices

`RGUX-1` and `RGUX-2` can execute independently now. In parallel, the Harness repository owns the public request-contract extension required to unblock `RGUX-3`.

### RGUX-1 acceptance

- Android brand contract uses the RedactGuard desktop visual identity: primary blue `#004AC6`, cool layered surfaces, desktop-aligned light/dark semantic palette and PII category accents.
- A durable RedactGuard mark is repository-owned for Android use; launcher/icon work reuses the approved visual source rather than inventing a second logo.
- Tokens/components contain no scattered raw brand values outside the canonical theme owners.

Validation:

- `./gradlew spotlessCheck :app:lintDebug`
- Compose/theme focused tests where applicable.

### RGUX-2 acceptance

- Built-ins cover the desktop taxonomy needed by General, Financial, Healthcare and Legal profiles.
- Each built-in has a stable ID, user label, bounded prompt description, semantic category and optional examples suitable for request-time prompt context.
- Profiles are deterministic RedactGuard bundles and never reuse the Harness inference-preset type/identity.
- Existing six Android types map without ambiguous aliases; migration does not persist sensitive values.

Validation:

- `./gradlew :app:testDebugUnitTest`
- focused taxonomy/profile tests.

### RGUX-3 acceptance

- RedactGuard sends only selected product-owned PII definition descriptors through the Consumer SDK.
- Request metadata is bounded/validated and content-safe in `toString`/diagnostics.
- Harness combines trusted use-case system instructions with the request PII descriptors deterministically; request metadata never replaces the host-owned base prompt.
- Binder/API compatibility and direct consumer tests cover serialization, bounds and missing/invalid definitions.

Validation:

- RedactGuard targeted adapter tests.
- Harness `core/contracts`, Binder contract/client/host and Consumer SDK tests.
- Cross-APK device evidence remains `PENDING` until executed on a physical device.

## Screen contract

### Document

Primary action: `Importa un PDF`.
Secondary: `Incolla testo`.
Show a compact local-AI/privacy status without technical implementation vocabulary. Image-only PDF limitation remains contextual copy.

### Protection

First choose a RedactGuard profile (`Generale`, `Sanitario`, `Finanziario`, `Legale`) or customize categories. Profile selection sets deterministic type selection; granular toggles remain editable. Host-published inference mode, when multiple options exist, is shown separately under progressive disclosure and never labelled as a PII profile.

### Analysis

Show real phases: document prepared -> sensitive-data search -> result validation. Determinate progress is optional and only derived from real page/chunk/work-unit state. Cancellation remains first-class.

### Review

Show category, masked contextual excerpt, source position/segment where available, hidden current value, explicit reveal, and `Oscura` / `Mantieni` decision hierarchy. Phone uses one focused decision surface; expanded windows use context + decision panes. No raw PDF coordinate viewer in this wave.

### Outcome / export

Show reviewed/redacted/maintained counts when available, privacy confirmation and protected-file outcome. Primary action exports/opens the protected result according to existing supported capabilities; `Nuovo documento` is secondary.

## Integration points

- `design/ux-contract.json` remains the durable experience owner; this workstream only coordinates execution.
- `design/brand-kit.json` owns visual tokens and brand assets.
- `domain/pii` owns PII taxonomy/profile policy.
- Harness `core/contracts` owns the generic public request descriptor contract; Binder/Consumer SDK mirror that contract without RedactGuard-specific taxonomy knowledge.
- Existing Harness inference preset selection remains an independent control-plane concern and is visually/semantically separated from PII profile selection.

## Durable documentation destinations

- `design/ux-contract.json`: final task/IA/review/profile/adaptive semantics.
- `design/brand-kit.json`: final brand, semantic PII colors, typography, shapes and motion.
- `docs/features/`: only if expanded PII/request behavior needs durable feature-level explanation beyond executable contracts/tests.
- tests/contracts: taxonomy/profile membership, safe review projection, request bounds and cross-boundary compatibility.

## Completion

Complete only when code, design contracts, Consumer SDK integration, accessibility/adaptive behavior and validation agree. Physical-device TalkBack/large-text/adaptive/two-APK evidence may be the final bounded gate and must remain `PENDING` until actually executed. Transfer durable truth, update `docs/current-state.md`, then delete this workstream by default.